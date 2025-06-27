import csv
import pandas as pd
from collections import defaultdict

def generate_avoid_list(original_csv, modified_csv, output_csv):
    """生成避坑列表：对比两个CSV的差异"""
    print("⏳ 正在分析CSV差异...")
    
    # 读取CSV文件
    try:
        df_orig = pd.read_csv(original_csv)
        df_mod = pd.read_csv(modified_csv)
    except Exception as e:
        print(f"❌ 文件读取失败: {str(e)}")
        return
    
    # 基于ID创建索引
    orig_dict = {row['id']: row for _, row in df_orig.iterrows()}
    mod_dict = {row['id']: row for _, row in df_mod.iterrows()}
    
    # 识别差异项
    pitfalls = []
    for item_id, orig_row in orig_dict.items():
        if item_id not in mod_dict:
            pitfalls.append(orig_row)  # 完全删除的项
        else:
            mod_row = mod_dict[item_id]
            # 检查关键字段差异
            diff_fields = []
            for col in ['alphabet', 'han_num', 'chinese_name']:
                if orig_row[col] != mod_row[col]:
                    diff_fields.append(col)
            if diff_fields:
                pitfalls.append({**orig_row, 'modified_fields': ','.join(diff_fields)})
    
    # 保存避坑列表
    if pitfalls:
        pd.DataFrame(pitfalls).to_csv(output_csv, index=False)
        print(f"✅ 已生成避坑列表: {output_csv} ({len(pitfalls)}个风险项)")
    else:
        print("ℹ️ 未发现需要避坑的差异项")

def auto_remove_pitfalls(original_csv, avoid_csv, output_csv):
    """自动删除避坑项"""
    print("⏳ 正在执行自动删坑...")
    
    # 读取数据
    df_orig = pd.read_csv(original_csv)
    df_avoid = pd.read_csv(avoid_csv)
    
    # 创建避坑ID索引
    avoid_ids = set(df_avoid['id'])
    avoid_dict = {row['id']: row for _, row in df_avoid.iterrows()}
    
    # 执行清理
    success_removed = []
    unmatched = []
    
    cleaned_rows = []
    for _, row in df_orig.iterrows():
        item_id = row['id']
        if item_id in avoid_ids:
            # 记录成功移除项
            risk_data = avoid_dict[item_id]
            modified_fields = risk_data.get('modified_fields', '完全删除')
            success_removed.append({
                'id': item_id,
                'name': risk_data['chinese_name'],
                'risk_type': modified_fields
            })
        else:
            cleaned_rows.append(row)
    
    # 检查未匹配项
    processed_ids = {row['id'] for row in cleaned_rows} | {item['id'] for item in success_removed}
    for item_id in avoid_ids:
        if item_id not in processed_ids:
            unmatched.append({
                'id': item_id,
                'reason': '原始CSV中不存在此ID'
            })
    
    # 保存清理结果
    pd.DataFrame(cleaned_rows).to_csv(output_csv, index=False)
    
    # 控制台报告
    print("\n=== 清理结果报告 ===")
    if success_removed:
        print(f"✅ 成功避坑项 ({len(success_removed)}个):")
        for item in success_removed:
            print(f"   - {item['id']} ({item['name']}) 风险类型: {item['risk_type']}")
    else:
        print("ℹ️ 未找到需清理的匹配项")
    
    if unmatched:
        print(f"❌ 未匹配项 ({len(unmatched)}个):")
        for item in unmatched:
            print(f"   - {item['id']}: {item['reason']}")
    
    print(f"\n💾 清理后文件已保存: {output_csv}")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='Minecraft物品数据避坑工具')
    subparsers = parser.add_subparsers(dest='command', required=True)
    
    # generate命令
    parser_gen = subparsers.add_parser('generate', help='生成避坑列表')
    parser_gen.add_argument('--original', required=True, help='原始CSV文件路径')
    parser_gen.add_argument('--modified', required=True, help='修改后CSV文件路径')
    parser_gen.add_argument('--output', required=True, help='避坑列表输出路径')
    
    # remove命令
    parser_rm = subparsers.add_parser('remove', help='自动删除避坑项')
    parser_rm.add_argument('--original', required=True, help='原始CSV文件路径')
    parser_rm.add_argument('--avoid', required=True, help='避坑列表文件路径')
    parser_rm.add_argument('--output', required=True, help='清理后输出路径')
    
    args = parser.parse_args()
    
    if args.command == 'generate':
        generate_avoid_list(args.original, args.modified, args.output)
    elif args.command == 'remove':
        auto_remove_pitfalls(args.original, args.avoid, args.output)
