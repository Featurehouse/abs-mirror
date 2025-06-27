import argparse
import subprocess
import json
import csv
import os
import requests
import sys
from collections import defaultdict
import unicodedata

def download_server_jar(version):
    # 获取版本清单
    manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    manifest = requests.get(manifest_url).json()

    # 查找版本元数据URL
    version_meta_url = None
    for entry in manifest["versions"]:
        if entry["id"] == version:
            version_meta_url = entry["url"]
            break

    if not version_meta_url:
        raise Exception(f"找不到版本 {version} 的元数据")

    # 获取server.jar下载URL
    version_meta = requests.get(version_meta_url).json()
    server_url = version_meta["downloads"]["server"]["url"]

    # 下载文件
    os.makedirs("server_files", exist_ok=True)
    jar_path = f"server_files/server_{version}.jar"

    print(f"⏳ 正在下载 {version} 的 server.jar...")
    response = requests.get(server_url)
    with open(jar_path, "wb") as f:
        f.write(response.content)
    print(f"✅ server.jar 已下载到 {jar_path}")

    return jar_path

# ========================
# CSV生成模块
# ========================
def generate_csv(version, output_csv):
    """生成中间CSV文件：执行server.jar并处理数据"""
    server_jar = download_server_jar(version)
    try:
        # 1. 调用server.jar生成物品报告
        print(f"⏳ 正在执行server.jar生成物品报告...")
        cmd = [
            'java',
            '-DbundlerMainClass=net.minecraft.data.Main',
            '-jar', server_jar,
            '--reports'
        ]
        subprocess.run(cmd, check=True, capture_output=True)
        print("✅ server.jar执行成功")
    except subprocess.CalledProcessError as e:
        print(f"❌ server.jar执行失败: {e.stderr.decode()}")
        sys.exit(1)
    except FileNotFoundError:
        print("❌ 找不到server.jar文件，请确认路径是否正确")
        sys.exit(1)

    # 2. 获取中文翻译文件
    print("⏳ 正在获取中文翻译文件...")
    try:
        manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        manifest = requests.get(manifest_url).json()

        # 查找版本元数据
        version_meta_url = next((v['url'] for v in manifest['versions'] if v['id'] == version), None)
        if not version_meta_url:
            print(f"❌ 找不到版本 {version} 的元数据")
            sys.exit(1)

        version_meta = requests.get(version_meta_url).json()
        asset_index_url = version_meta['assetIndex']['url']
        asset_index = requests.get(asset_index_url).json()
        zh_cn_hash = asset_index['objects']['minecraft/lang/zh_cn.json']['hash']
        zh_cn_url = f"https://resources.download.minecraft.net/{zh_cn_hash[:2]}/{zh_cn_hash}"
        zh_cn_data = requests.get(zh_cn_url).json()
        print("✅ 中文翻译文件获取成功")
    except Exception as e:
        print(f"❌ 获取中文翻译文件失败: {str(e)}")
        sys.exit(1)

    # 3. 处理物品报告并生成CSV
    print("⏳ 正在处理物品数据生成CSV...")
    items_report_path = "generated/reports/items.json"
    csv_data = []

    with open(items_report_path, 'r', encoding='utf-8') as f:
        items_data = json.load(f)

    for item_id, item_info in items_data.items():
        base_id = item_id.split(':')[1]  # 移除minecraft:前缀
        translate_key = item_info['components']['minecraft:item_name']['translate']
        chinese_name = zh_cn_data.get(translate_key, '')

        if chinese_name:
            # 清洗非字母数字字符
            cleaned_name = ''.join(c for c in chinese_name if unicodedata.category(c)[0] in 'LN')
            char_count = len(cleaned_name)
            ##initial = cleaned_name[0].upper() if cleaned_name else '?'
            initial = base_id[0].upper()

            csv_data.append({
                'alphabet': initial,
                'id': base_id,
                'han_num': char_count,
                'chinese_name': chinese_name
            })

    # 4. 写入CSV文件
    with open(output_csv, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=['alphabet', 'id', 'han_num', 'chinese_name'])
        writer.writeheader()
        writer.writerows(csv_data)

    print(f"✅ CSV文件已生成: {output_csv}")
    print("💡 请手动检查并移除CSV中不可用的项目")

# ========================
# 数据包生成模块
# ========================
def generate_datapack(input_csv, output_dir):
    """从CSV文件生成数据包"""
    # 1. 读取CSV数据
    try:
        print(f"⏳ 正在读取CSV文件: {input_csv}")
        items = []
        with open(input_csv, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                items.append(row)
        print(f"✅ 成功读取 {len(items)} 个物品数据")
    except FileNotFoundError:
        print(f"❌ 找不到CSV文件: {input_csv}")
        sys.exit(1)

    # 2. 创建数据包目录结构
    print("⏳ 正在创建数据包结构...")
    speedabc_dir = os.path.join(output_dir, 'data', 'speedabc', 'tags', 'items')
    hannumspeed_dir = os.path.join(output_dir, 'data', 'hannumspeed', 'tags', 'items')
    os.makedirs(speedabc_dir, exist_ok=True)
    os.makedirs(hannumspeed_dir, exist_ok=True)

    # 3. 分组物品数据
    speedabc_tags = defaultdict(list)
    hannum_tags = defaultdict(list)

    for item in items:
        # SpeedABC分组 (按首字母)
        letter = item['alphabet'].lower()
        speedabc_tags[letter].append(f"minecraft:{item['id']}")

        # HanNumSpeed分组 (按字数)
        length = item['han_num']
        hannum_tags[length].append(f"minecraft:{item['id']}")

    # 4. 生成SpeedABC标签文件
    print("⏳ 生成SpeedABC标签...")
    for letter, ids in speedabc_tags.items():
        # 文件名过滤非法字符
        safe_letter = ''.join(c for c in letter if c.isalnum())
        if not safe_letter:
            safe_letter = 'OTHER'

        file_path = os.path.join(speedabc_dir, f"{safe_letter}.json")
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump({
                "replace": False,
                "values": ids
            }, f, indent=2, ensure_ascii=False)

    # 5. 生成HanNumSpeed标签文件
    print("⏳ 生成HanNumSpeed标签...")
    for length, ids in hannum_tags.items():
        file_path = os.path.join(hannumspeed_dir, f"len_{length}.json")
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump({
                "replace": False,
                "values": ids
            }, f, indent=2, ensure_ascii=False)

    print(f"✅ 数据包已生成至: {output_dir}")
    print("💡 注意：请手动添加pack.mcmeta文件以使数据包可用")

# ========================
# 主程序
# ========================
def main():
    parser = argparse.ArgumentParser(description='Minecraft物品分类数据包生成工具')
    subparsers = parser.add_subparsers(dest='command', required=True)

    # generate-csv 命令
    parser_csv = subparsers.add_parser('csv', help='生成中间CSV文件')
    parser_csv.add_argument('--version', required=True, help='Minecraft版本号，如1.21.6')
    parser_csv.add_argument('--output-csv', required=True, help='输出的CSV文件路径')

    # generate-datapack 命令
    parser_datapack = subparsers.add_parser('datapack', help='生成最终数据包')
    parser_datapack.add_argument('--input-csv', required=True, help='输入的CSV文件路径')
    parser_datapack.add_argument('--output-dir', required=True, help='数据包输出目录')

    args = parser.parse_args()

    if args.command == 'csv':
        generate_csv(args.version, args.output_csv)
    elif args.command == 'datapack':
        generate_datapack(args.input_csv, args.output_dir)

if __name__ == '__main__':
    main()