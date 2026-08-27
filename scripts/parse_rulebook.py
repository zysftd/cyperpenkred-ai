#!/usr/bin/env python3
"""
Cyberpunk Red Rulebook PDF Parser
Extracts text from PDF and structures it into JSON for the AI knowledge base.
"""

import subprocess
import json
import re
import os

def extract_text_from_pdf(pdf_path, output_path):
    """Extract text from PDF using pdftotext"""
    cmd = ['pdftotext', '-layout', pdf_path, output_path]
    subprocess.run(cmd, check=True)
    with open(output_path, 'r', encoding='utf-8') as f:
        return f.read()

def parse_rulebook(text):
    """Parse extracted text into structured rulebook entries"""
    entries = []
    
    # Split by major sections
    sections = re.split(r'\n(?=\d+\s|[一二三四五六七八九十]+\s|[ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩ]\s)', text)
    
    current_category = "通用"
    current_section = ""
    
    for section in sections:
        lines = section.strip().split('\n')
        if not lines:
            continue
            
        # Try to detect category
        first_line = lines[0].strip()
        
        # Detect major categories
        if '角色' in first_line or '生命路径' in first_line:
            current_category = "角色创建"
        elif '战斗' in first_line or '远程' in first_line or '近身' in first_line:
            current_category = "战斗系统"
        elif '武器' in first_line or '枪' in first_line:
            current_category = "装备"
        elif '护甲' in first_line:
            current_category = "装备"
        elif '义体' in first_line or '赛博' in first_line:
            current_category = "义体系统"
        elif '网行' in first_line or '网络' in first_line:
            current_category = "网行系统"
        elif '夜之城' in first_line:
            current_category = "世界设定"
        elif 'GM' in first_line or '主持' in first_line:
            current_category = "GM指南"
        
        # Create entry for significant chunks
        content = '\n'.join(lines[:20])  # First 20 lines as content
        if len(content) > 50:  # Only include substantial content
            entry = {
                'category': current_category,
                'section': current_section,
                'title': first_line[:100],
                'content': content,
                'keywords': extract_keywords(content),
                'page_number': None
            }
            entries.append(entry)
    
    return entries

def extract_keywords(text):
    """Extract relevant keywords from text"""
    keyword_patterns = {
        '射击': ['射击', '远程', '枪', '手枪', '步枪', '霰弹枪', '冲锋枪'],
        '近战': ['近战', '刀', '剑', '格斗', '搏击', '拳'],
        '伤害': ['伤害', 'HP', '生命值', '损伤', '伤害骰'],
        '护甲': ['护甲', 'SP', '装甲', '防弹'],
        '骰点': ['骰点', '判定', '检定', '投骰', '1d10'],
        '先攻': ['先攻', '回合', '行动顺序'],
        '义体': ['义体', '赛博', '植入', '改造', '赛博组件'],
        '网行': ['网行', '网络', '黑客', '程序', '黑冰'],
        '技能': ['技能', '能力', '专长', '技能点'],
        '属性': ['属性', 'INT', 'REF', 'DEX', 'TECH', 'COOL', 'ATTR', 'LUCK', 'MA', 'BODY', 'EMP'],
        '角色': ['角色', '职业', '摇滚小子', '佣兵', '网行者', '技工', '技医', '媒体人', '主管', '执法者', '掮客', '游民'],
        '战斗': ['战斗', '攻击', '防御', '闪避', '掩护', '瞄准'],
        '装备': ['装备', '武器', '弹药', '工具'],
        '治疗': ['治疗', '医疗', '创伤', '恢复'],
        '死亡': ['死亡', '豁免', '濒死']
    }
    
    found_keywords = []
    text_lower = text.lower()
    
    for category, keywords in keyword_patterns.items():
        for keyword in keywords:
            if keyword.lower() in text_lower:
                found_keywords.append(category)
                found_keywords.append(keyword)
                break
    
    return list(set(found_keywords))[:10]

def save_to_json(entries, output_path):
    """Save entries to JSON file"""
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(entries, f, ensure_ascii=False, indent=2)

def main():
    pdf_path = 'others/赛博朋克红2.50.15规则书精修版.pdf'
    text_path = '/tmp/rulebook_full.txt'
    json_path = 'app/src/main/assets/rulebook_entries.json'
    
    print("Extracting text from PDF...")
    text = extract_text_from_pdf(pdf_path, text_path)
    
    print(f"Text extracted: {len(text)} characters")
    
    print("Parsing rulebook...")
    entries = parse_rulebook(text)
    
    print(f"Found {len(entries)} entries")
    
    # Create assets directory
    os.makedirs(os.path.dirname(json_path), exist_ok=True)
    
    print("Saving to JSON...")
    save_to_json(entries, json_path)
    
    print(f"Done! Saved to {json_path}")
    
    # Print sample entries
    print("\nSample entries:")
    for entry in entries[:5]:
        print(f"  - [{entry['category']}] {entry['title'][:50]}...")
        print(f"    Keywords: {entry['keywords'][:5]}")

if __name__ == '__main__':
    main()
