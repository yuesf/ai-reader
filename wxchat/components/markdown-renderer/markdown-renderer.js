// components/markdown-renderer/markdown-renderer.js
Component({
  properties: {
    content: {
      type: String,
      value: '',
      observer: function(newVal) {
        this.parseMarkdown(newVal);
      }
    }
  },

  data: {
    parsedContent: []
  },

  methods: {
    parseMarkdown(markdown) {
      if (!markdown) {
        this.setData({ parsedContent: [] });
        return;
      }

      const lines = markdown.split('\n');
      const parsedContent = [];
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const element = this.parseLine(line);
        if (element) {
          parsedContent.push(element);
        }
      }

      this.setData({ parsedContent });
    },

    parseLine(line) {
      // 空行
      if (!line.trim()) {
        return { type: 'br', content: '' };
      }

      // 标题 (# ## ### #### ##### ######)
      const headerMatch = line.match(/^(#{1,6})\s+(.+)$/);
      if (headerMatch) {
        const level = headerMatch[1].length;
        const content = headerMatch[2];
        return { 
          type: 'header', 
          level: level, 
          content: this.parseInlineElements(content) 
        };
      }

      // 无序列表 (- * +)
      const unorderedListMatch = line.match(/^[\s]*[-*+]\s+(.+)$/);
      if (unorderedListMatch) {
        return { 
          type: 'list-item', 
          ordered: false, 
          content: this.parseInlineElements(unorderedListMatch[1]) 
        };
      }

      // 有序列表 (1. 2. 3.)
      const orderedListMatch = line.match(/^[\s]*\d+\.\s+(.+)$/);
      if (orderedListMatch) {
        return { 
          type: 'list-item', 
          ordered: true, 
          content: this.parseInlineElements(orderedListMatch[1]) 
        };
      }

      // 引用 (>)
      const blockquoteMatch = line.match(/^>\s+(.+)$/);
      if (blockquoteMatch) {
        return { 
          type: 'blockquote', 
          content: this.parseInlineElements(blockquoteMatch[1]) 
        };
      }

      // 代码块 (```)
      if (line.trim().startsWith('```')) {
        return { type: 'code-block', content: line.replace(/```/g, '') };
      }

      // 分割线 (--- *** ___)
      if (line.match(/^[-*_]{3,}$/)) {
        return { type: 'hr', content: '' };
      }

      // 普通段落
      return { 
        type: 'paragraph', 
        content: this.parseInlineElements(line) 
      };
    },

    parseInlineElements(text) {
      const elements = [];
      let currentText = text;
      
      // 处理行内元素的正则表达式
      const patterns = [
        { type: 'bold', regex: /\*\*(.*?)\*\*/g },
        { type: 'italic', regex: /\*(.*?)\*/g },
        { type: 'code', regex: /`(.*?)`/g },
        { type: 'link', regex: /\[([^\]]+)\]\(([^)]+)\)/g }
      ];

      let lastIndex = 0;
      const matches = [];

      // 收集所有匹配项
      patterns.forEach(pattern => {
        let match;
        while ((match = pattern.regex.exec(text)) !== null) {
          matches.push({
            type: pattern.type,
            start: match.index,
            end: match.index + match[0].length,
            content: match[1],
            url: match[2] || null,
            original: match[0]
          });
        }
      });

      // 按位置排序
      matches.sort((a, b) => a.start - b.start);

      // 构建元素数组
      matches.forEach(match => {
        // 添加匹配前的普通文本
        if (match.start > lastIndex) {
          const plainText = text.substring(lastIndex, match.start);
          if (plainText) {
            elements.push({ type: 'text', content: plainText });
          }
        }

        // 添加匹配的元素
        elements.push({
          type: match.type,
          content: match.content,
          url: match.url
        });

        lastIndex = match.end;
      });

      // 添加剩余的普通文本
      if (lastIndex < text.length) {
        const remainingText = text.substring(lastIndex);
        if (remainingText) {
          elements.push({ type: 'text', content: remainingText });
        }
      }

      // 如果没有任何匹配，返回整个文本作为普通文本
      if (elements.length === 0) {
        elements.push({ type: 'text', content: text });
      }

      return elements;
    },

    onLinkTap(e) {
      const url = e.currentTarget.dataset.url;
      if (url) {
        // 在小程序中处理链接点击
        wx.showModal({
          title: '打开链接',
          content: `是否要打开链接：${url}`,
          success: (res) => {
            if (res.confirm) {
              // 可以在这里处理链接跳转逻辑
              wx.setClipboardData({
                data: url,
                success: () => {
                  wx.showToast({
                    title: '链接已复制到剪贴板',
                    icon: 'success'
                  });
                }
              });
            }
          }
        });
      }
    }
  }
});