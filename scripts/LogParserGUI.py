#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
import re
import os
import sys
import webbrowser
from datetime import datetime
import html
import threading

class LogParser:
    def __init__(self):
        self.sections = []
        self.current_section = None
        
    def parse_log_file(self, file_path):
        """解析日志文件"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
        except UnicodeDecodeError:
            try:
                with open(file_path, 'r', encoding='gbk') as f:
                    content = f.read()
            except:
                return None, "无法解码文件编码，请检查文件格式"
        
        lines = content.split('\n')
        self.sections = []
        self.current_section = None
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
                
            # 检测章节分隔线
            if re.match(r'^-+$', line):
                if self.current_section and self.current_section['content']:
                    self.sections.append(self.current_section)
                self.current_section = {'title': '', 'content': [], 'type': 'normal'}
                continue
            
            # 检测章节标题
            if self.current_section and not self.current_section['title']:
                # 跳过颜色代码和特殊字符
                clean_line = re.sub(r'\x1b\[[0-9;]*m', '', line)
                if clean_line and not re.match(r'^-+$', clean_line):
                    self.current_section['title'] = clean_line
                    # 检测检查结果类型
                    if '[+]' in line or 'Compliant' in line or '合格' in line:
                        self.current_section['type'] = 'pass'
                    elif '[-]' in line or 'Non-Compliant' in line or '不合格' in line:
                        self.current_section['type'] = 'fail'
            else:
                if self.current_section:
                    # 清理ANSI颜色代码
                    clean_line = re.sub(r'\x1b\[[0-9;]*m', '', line)
                    if clean_line:
                        self.current_section['content'].append(clean_line)
        
        # 添加最后一个章节
        if self.current_section and self.current_section['content']:
            self.sections.append(self.current_section)
            
        return self.sections, "解析成功"

class HTMLGenerator:
    def __init__(self):
        self.css_style = """
        <style>
            body {
                font-family: Arial, sans-serif;
                margin: 20px;
                background-color: #f5f5f5;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                padding: 20px;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 20px;
                border-radius: 8px;
                margin-bottom: 20px;
            }
            .section {
                margin-bottom: 15px;
                border: 1px solid #ddd;
                border-radius: 5px;
                overflow: hidden;
            }
            .section-title {
                padding: 10px 15px;
                font-weight: bold;
                cursor: pointer;
                background-color: #f8f9fa;
                border-bottom: 1px solid #ddd;
            }
            .section.pass .section-title {
                background-color: #d4edda;
                color: #155724;
            }
            .section.fail .section-title {
                background-color: #f8d7da;
                color: #721c24;
            }
            .section-content {
                padding: 15px;
                background-color: white;
                display: none;
            }
            .section-content pre {
                margin: 0;
                white-space: pre-wrap;
                font-family: 'Courier New', monospace;
                font-size: 12px;
                line-height: 1.4;
            }
            .summary {
                background-color: #e9ecef;
                padding: 15px;
                border-radius: 5px;
                margin-bottom: 20px;
            }
            .statistics {
                display: flex;
                justify-content: space-around;
                margin: 20px 0;
            }
            .stat-item {
                text-align: center;
                padding: 10px;
            }
            .stat-number {
                font-size: 24px;
                font-weight: bold;
            }
            .pass-count { color: #28a745; }
            .fail-count { color: #dc3545; }
            .total-count { color: #007bff; }
            .search-box {
                margin-bottom: 20px;
                padding: 10px;
                width: 100%;
                box-sizing: border-box;
            }
            .timestamp {
                color: #6c757d;
                font-size: 14px;
                text-align: right;
            }
        </style>
        """
        
        self.javascript = """
        <script>
            function toggleSection(sectionId) {
                var content = document.getElementById('content-' + sectionId);
                if (content.style.display === 'block') {
                    content.style.display = 'none';
                } else {
                    content.style.display = 'block';
                }
            }
            
            function searchSections() {
                var input = document.getElementById('searchInput');
                var filter = input.value.toLowerCase();
                var sections = document.getElementsByClassName('section');
                
                for (var i = 0; i < sections.length; i++) {
                    var title = sections[i].getElementsByClassName('section-title')[0];
                    var txtValue = title.textContent || title.innerText;
                    if (txtValue.toLowerCase().indexOf(filter) > -1) {
                        sections[i].style.display = "";
                    } else {
                        sections[i].style.display = "none";
                    }
                }
            }
            
            function expandAll() {
                var contents = document.getElementsByClassName('section-content');
                for (var i = 0; i < contents.length; i++) {
                    contents[i].style.display = 'block';
                }
            }
            
            function collapseAll() {
                var contents = document.getElementsByClassName('section-content');
                for (var i = 0; i < contents.length; i++) {
                    contents[i].style.display = 'none';
                }
            }
        </script>
        """
    
    def generate_html(self, sections, output_file, original_filename):
        """生成HTML报告"""
        # 统计信息
        pass_count = sum(1 for s in sections if s['type'] == 'pass')
        fail_count = sum(1 for s in sections if s['type'] == 'fail')
        total_count = len(sections)
        
        html_content = f"""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Linux Baseline Check Report</title>
            {self.css_style}
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🐧 Linux Baseline Configuration Check Report</h1>
                    <p>Source: {html.escape(original_filename)}</p>
                    <p class="timestamp">Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
                </div>
                
                <div class="summary">
                    <h3>📊 Executive Summary</h3>
                    <div class="statistics">
                        <div class="stat-item">
                            <div class="stat-number total-count">{total_count}</div>
                            <div>Total Checks</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-number pass-count">{pass_count}</div>
                            <div>Passed</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-number fail-count">{fail_count}</div>
                            <div>Failed</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-number">{round(pass_count/total_count*100 if total_count > 0 else 0, 1)}%</div>
                            <div>Success Rate</div>
                        </div>
                    </div>
                </div>
                
                <div style="margin-bottom: 20px;">
                    <input type="text" id="searchInput" class="search-box" 
                           placeholder="🔍 Search sections..." onkeyup="searchSections()">
                    <button onclick="expandAll()" style="margin-right: 10px;">Expand All</button>
                    <button onclick="collapseAll()">Collapse All</button>
                </div>
        """
        
        # 添加各个章节
        for i, section in enumerate(sections):
            section_id = f"section-{i}"
            content_id = f"content-{i}"
            
            # 转义HTML特殊字符
            title = html.escape(section['title'])
            content = '\n'.join(html.escape(line) for line in section['content'])
            
            html_content += f"""
                <div class="section {section['type']}">
                    <div class="section-title" onclick="toggleSection({i})">
                        {'✅' if section['type'] == 'pass' else '❌'} {title}
                    </div>
                    <div class="section-content" id="{content_id}">
                        <pre>{content}</pre>
                    </div>
                </div>
            """
        
        html_content += f"""
                {self.javascript}
            </div>
        </body>
        </html>
        """
        
        # 写入文件
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(html_content)
            return output_file, None
        except Exception as e:
            return None, f"生成HTML文件时出错: {str(e)}"

class LogParserGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Linux基线检查日志分析器 v1.0")
        self.root.geometry("800x600")
        self.root.resizable(True, True)
        
        # 设置图标（如果有）
        try:
            self.root.iconbitmap("icon.ico")  # 如果有图标文件
        except:
            pass
        
        # 初始化解析器和生成器
        self.parser = LogParser()
        self.html_gen = HTMLGenerator()
        
        # 当前处理的文件
        self.current_file = None
        self.sections = []
        
        # 创建界面
        self.create_widgets()
        
        # 状态变量
        self.processing = False
        
    def create_widgets(self):
        # 创建主框架
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # 配置网格权重
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        main_frame.rowconfigure(3, weight=1)
        
        # 标题
        title_label = ttk.Label(main_frame, text="Linux基线检查日志分析器", 
                               font=("Arial", 16, "bold"))
        title_label.grid(row=0, column=0, columnspan=3, pady=(0, 20))
        
        # 文件选择区域
        file_frame = ttk.LabelFrame(main_frame, text="文件选择", padding="10")
        file_frame.grid(row=1, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=(0, 10))
        file_frame.columnconfigure(1, weight=1)
        
        ttk.Label(file_frame, text="日志文件:").grid(row=0, column=0, sticky=tk.W, padx=(0, 10))
        
        self.file_path = tk.StringVar()
        file_entry = ttk.Entry(file_frame, textvariable=self.file_path, width=50)
        file_entry.grid(row=0, column=1, sticky=(tk.W, tk.E), padx=(0, 10))
        
        browse_btn = ttk.Button(file_frame, text="浏览...", command=self.browse_file)
        browse_btn.grid(row=0, column=2, padx=(0, 10))
        
        parse_btn = ttk.Button(file_frame, text="解析文件", command=self.parse_file)
        parse_btn.grid(row=0, column=3)
        
        # 统计信息区域
        stats_frame = ttk.LabelFrame(main_frame, text="统计信息", padding="10")
        stats_frame.grid(row=2, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # 使用网格布局统计信息
        self.total_label = ttk.Label(stats_frame, text="总检查项: 0")
        self.total_label.grid(row=0, column=0, padx=20)
        
        self.pass_label = ttk.Label(stats_frame, text="通过: 0", foreground="green")
        self.pass_label.grid(row=0, column=1, padx=20)
        
        self.fail_label = ttk.Label(stats_frame, text="失败: 0", foreground="red")
        self.fail_label.grid(row=0, column=2, padx=20)
        
        self.rate_label = ttk.Label(stats_frame, text="通过率: 0%")
        self.rate_label.grid(row=0, column=3, padx=20)
        
        # 日志内容区域
        log_frame = ttk.LabelFrame(main_frame, text="日志内容预览", padding="10")
        log_frame.grid(row=3, column=0, columnspan=3, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 10))
        log_frame.columnconfigure(0, weight=1)
        log_frame.rowconfigure(0, weight=1)
        
        self.log_text = scrolledtext.ScrolledText(log_frame, width=80, height=20, wrap=tk.WORD)
        self.log_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # 按钮区域
        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=4, column=0, columnspan=3, pady=(10, 0))
        
        self.generate_btn = ttk.Button(button_frame, text="生成HTML报告", 
                                      command=self.generate_html, state=tk.DISABLED)
        self.generate_btn.pack(side=tk.LEFT, padx=(0, 10))
        
        self.open_btn = ttk.Button(button_frame, text="打开报告", 
                                  command=self.open_report, state=tk.DISABLED)
        self.open_btn.pack(side=tk.LEFT, padx=(0, 10))
        
        ttk.Button(button_frame, text="退出", command=self.root.quit).pack(side=tk.LEFT)
        
        # 状态栏
        self.status_var = tk.StringVar()
        self.status_var.set("就绪")
        status_bar = ttk.Label(main_frame, textvariable=self.status_var, relief=tk.SUNKEN)
        status_bar.grid(row=5, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=(10, 0))
        
        # 进度条
        self.progress = ttk.Progressbar(main_frame, mode='indeterminate')
        self.progress.grid(row=6, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=(5, 0))
        
    def browse_file(self):
        filename = filedialog.askopenfilename(
            title="选择日志文件",
            filetypes=[("文本文件", "*.txt"), ("所有文件", "*.*")]
        )
        if filename:
            self.file_path.set(filename)
            self.current_file = filename
            self.status_var.set(f"已选择文件: {os.path.basename(filename)}")
            
            # 预览文件内容
            self.preview_file(filename)
    
    def preview_file(self, filename):
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                content = f.read(5000)  # 只预览前5000个字符
            self.log_text.delete(1.0, tk.END)
            self.log_text.insert(1.0, content)
        except:
            try:
                with open(filename, 'r', encoding='gbk') as f:
                    content = f.read(5000)
                self.log_text.delete(1.0, tk.END)
                self.log_text.insert(1.0, content)
            except Exception as e:
                self.log_text.delete(1.0, tk.END)
                self.log_text.insert(1.0, f"无法预览文件: {str(e)}")
    
    def parse_file(self):
        if not self.file_path.get():
            messagebox.showerror("错误", "请先选择日志文件")
            return
            
        # 禁用按钮，开始处理
        self.processing = True
        self.generate_btn.config(state=tk.DISABLED)
        self.open_btn.config(state=tk.DISABLED)
        self.progress.start(10)
        self.status_var.set("正在解析文件...")
        
        # 在新线程中处理文件，避免界面冻结
        thread = threading.Thread(target=self._parse_file_thread)
        thread.daemon = True
        thread.start()
    
    def _parse_file_thread(self):
        filename = self.file_path.get()
        sections, message = self.parser.parse_log_file(filename)
        
        # 在主线程中更新UI
        self.root.after(0, self._parse_file_complete, sections, message)
    
    def _parse_file_complete(self, sections, message):
        self.progress.stop()
        self.processing = False
        
        if sections is None:
            messagebox.showerror("解析错误", message)
            self.status_var.set("解析失败")
            return
            
        self.sections = sections
        
        # 更新统计信息
        total = len(sections)
        pass_count = sum(1 for s in sections if s['type'] == 'pass')
        fail_count = sum(1 for s in sections if s['type'] == 'fail')
        success_rate = round(pass_count/total*100, 1) if total > 0 else 0
        
        self.total_label.config(text=f"总检查项: {total}")
        self.pass_label.config(text=f"通过: {pass_count}")
        self.fail_label.config(text=f"失败: {fail_count}")
        self.rate_label.config(text=f"通过率: {success_rate}%")
        
        # 启用生成按钮
        self.generate_btn.config(state=tk.NORMAL)
        self.status_var.set(f"解析完成: {total} 个检查项")
        
        # 显示解析结果预览
        self.log_text.delete(1.0, tk.END)
        for i, section in enumerate(sections[:10]):  # 只显示前10个部分
            self.log_text.insert(tk.END, f"{i+1}. {section['title']}\n")
        
        if len(sections) > 10:
            self.log_text.insert(tk.END, f"... 还有 {len(sections)-10} 个部分未显示\n")
    
    def generate_html(self):
        if not self.sections:
            messagebox.showerror("错误", "没有可用的解析数据，请先解析文件")
            return
            
        # 选择输出文件
        output_file = filedialog.asksaveasfilename(
            title="保存HTML报告",
            defaultextension=".html",
            filetypes=[("HTML文件", "*.html"), ("所有文件", "*.*")]
        )
        
        if not output_file:
            return
            
        # 禁用按钮，开始处理
        self.processing = True
        self.generate_btn.config(state=tk.DISABLED)
        self.progress.start(10)
        self.status_var.set("正在生成HTML报告...")
        
        # 在新线程中生成HTML
        thread = threading.Thread(target=self._generate_html_thread, args=(output_file,))
        thread.daemon = True
        thread.start()
    
    def _generate_html_thread(self, output_file):
        result, error = self.html_gen.generate_html(
            self.sections, output_file, os.path.basename(self.current_file)
        )
        
        # 在主线程中更新UI
        self.root.after(0, self._generate_html_complete, result, error)
    
    def _generate_html_complete(self, result, error):
        self.progress.stop()
        self.processing = False
        
        if error:
            messagebox.showerror("生成错误", error)
            self.status_var.set("生成失败")
            return
            
        self.output_file = result
        self.open_btn.config(state=tk.NORMAL)
        self.status_var.set(f"报告已生成: {os.path.basename(result)}")
        messagebox.showinfo("成功", f"HTML报告已生成:\n{result}")
    
    def open_report(self):
        if hasattr(self, 'output_file') and os.path.exists(self.output_file):
            webbrowser.open('file://' + os.path.realpath(self.output_file))
        else:
            messagebox.showerror("错误", "没有可用的报告文件，请先生成报告")

def main():
    # 创建主窗口
    root = tk.Tk()
    
    # 设置主题（可选）
    try:
        # 尝试使用更现代的主题
        root.tk.call('source', 'azure.tcl')
        root.tk.call('set_theme', 'dark')
    except:
        # 如果失败，使用默认主题
        pass
    
    # 创建应用
    app = LogParserGUI(root)
    
    # 启动主循环
    root.mainloop()

if __name__ == "__main__":
    main()