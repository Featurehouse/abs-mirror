import os
import json
import requests
import subprocess
import argparse
from collections import defaultdict

# 全局路径设置
SERVER_DIR = "server_jars"
REPORTS_DIR = "item_reports"

def setup_directories():
    """创建必要目录"""
    os.makedirs(SERVER_DIR, exist_ok=True)
    os.makedirs(REPORTS_DIR, exist_ok=True)

def download_server_jar(version):
    """下载指定版本的server.jar"""
    # 获取版本清单
    manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    manifest = requests.get(manifest_url).json()
    
    # 查找版本元数据URL
    version_meta_url = next(
        (v["url"] for v in manifest["versions"] if v["id"] == version), None
    )
    if not version_meta_url:
        raise ValueError(f"找不到版本 {version} 的元数据")
    
    # 获取server.jar下载链接
    version_meta = requests.get(version_meta_url).json()
    server_url = version_meta["downloads"]["server"]["url"]
    jar_path = os.path.join(SERVER_DIR, f"server_{version}.jar")
    
    # 下载文件
    print(f"⏳ 下载 {version} 的 server.jar...")
    response = requests.get(server_url)
    with open(jar_path, "wb") as f:
        f.write(response.content)
    print(f"✅ 已保存至 {jar_path}")
    return jar_path

def generate_item_report(version, jar_path):
    """生成物品报告"""
    report_dir = os.path.join(REPORTS_DIR, version)
    os.makedirs(report_dir, exist_ok=True)
    report_path = os.path.join(report_dir, "reports", "items.json")
    
    # 执行server.jar生成报告
    print(f"⏳ 生成 {version} 的物品报告...")
    cmd = [
        "java",
        "-DbundlerMainClass=net.minecraft.data.Main",
        "-jar", jar_path,
        "--reports", "--output", report_dir
    ]
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL)
    print(f"✅ 报告已生成: {report_path}")
    return report_path

def load_item_data(report_path):
    """解析物品报告"""
    with open(report_path, "r") as f:
        data = json.load(f)
    return {
        item_id.split(":")[1]: item_data["components"]["minecraft:item_name"]["translate"]
        for item_id, item_data in data.items()
    }

def compare_items(version1, data1, version2, data2):
    """比较两个版本的物品差异"""
    results = {
        "added": {},      # 新增物品: {id: 翻译键}
        "removed": {},    # 删除物品: {id: 翻译键}
        "modified": {}    # 修改物品: {id: (旧翻译键, 新翻译键)}
    }
    
    # 检测新增和修改
    for item_id, trans_key2 in data2.items():
        if item_id not in data1:
            results["added"][item_id] = trans_key2
        elif data1[item_id] != trans_key2:
            results["modified"][item_id] = (data1[item_id], trans_key2)
    
    # 检测删除
    for item_id, trans_key1 in data1.items():
        if item_id not in data2:
            results["removed"][item_id] = trans_key1
    
    return results

def print_comparison(results, version1, version2):
    """可视化输出比较结果"""
    print(f"\n🔍 版本对比结果: {version1} → {version2}")
    
    # 输出新增物品
    if results["added"]:
        print(f"\n🟢 新增物品 ({len(results['added'])}):")
        for id, key in results["added"].items():
            print(f"  - {id}: {key}")
    
    # 输出删除物品
    if results["removed"]:
        print(f"\n🔴 删除物品 ({len(results['removed'])}):")
        for id, key in results["removed"].items():
            print(f"  - {id}: {key}")
    
    # 输出修改物品
    if results["modified"]:
        print(f"\n🟡 修改翻译键 ({len(results['modified'])}):")
        for id, (old_key, new_key) in results["modified"].items():
            print(f"  - {id}:")
            print(f"     旧: {old_key}")
            print(f"     新: {new_key}")
    
    # 统计总结
    total_changes = sum(len(v) for v in results.values())
    print(f"\n📊 总计变动: {total_changes} 项")

def main():
    parser = argparse.ArgumentParser(description="Minecraft物品报告对比工具")
    parser.add_argument("version1", help="第一个Minecraft版本号 (如 1.21.6)")
    parser.add_argument("version2", help="第二个Minecraft版本号 (如 1.21.7-rc2)")
    args = parser.parse_args()
    
    try:
        setup_directories()
        
        # 处理版本1
        jar1 = download_server_jar(args.version1)
        report1_path = generate_item_report(args.version1, jar1)
        data1 = load_item_data(report1_path)
        
        # 处理版本2
        jar2 = download_server_jar(args.version2)
        report2_path = generate_item_report(args.version2, jar2)
        data2 = load_item_data(report2_path)
        
        # 比较并输出结果
        results = compare_items(args.version1, data1, args.version2, data2)
        print_comparison(results, args.version1, args.version2)
        
    except Exception as e:
        print(f"❌ 错误: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
