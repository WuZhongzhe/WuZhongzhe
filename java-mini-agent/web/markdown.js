/**
 * 极简 Markdown 渲染器。
 * 智能体的回复通常是 Markdown 文本，直接插入 DOM 会把 **、# 等标记原样显示，
 * 这里把常用语法转成 HTML 后再交给浏览器渲染。
 *
 * 安全约定：先做 HTML 转义再做 Markdown 转换，因此模型返回内容中的 <script> 等标签不会被执行。
 */
(function (global) {
    'use strict';

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    /** 行内语法：代码、加粗、斜体、链接、删除线。 */
    function inline(text) {
        return text
            .replace(/`([^`]+)`/g, '<code class="md-inline">$1</code>')
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
            .replace(/__([^_]+)__/g, '<strong>$1</strong>')
            // 分隔符内侧不允许是空格，否则 "5 * 3" 这类算式会被误判成斜体
            .replace(/(^|[^*])\*(?!\s)([^*\n]*[^\s*])\*/g, '$1<em>$2</em>')
            .replace(/~~([^~]+)~~/g, '<del>$1</del>')
            .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g,
                '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    }

    /** 判断是否为表格行，例如 | 名称 | 说明 | */
    function isTableRow(line) {
        return /^\s*\|.*\|\s*$/.test(line) && line.split('|').length > 2;
    }

    /** 是否为表格分隔行，例如 | --- | --- | */
    function isTableDivider(line) {
        return /^\s*\|[\s:|-]+\|\s*$/.test(line) && line.indexOf('-') >= 0;
    }

    function cells(line) {
        return line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(function (cell) {
            return cell.trim();
        });
    }

    function renderTable(rows) {
        var html = '<table class="md-table">';
        var head = cells(rows[0]);
        html += '<thead><tr>' + head.map(function (cell) {
            return '<th>' + inline(cell) + '</th>';
        }).join('') + '</tr></thead><tbody>';
        for (var i = 1; i < rows.length; i++) {
            if (isTableDivider(rows[i])) {
                continue;
            }
            html += '<tr>' + cells(rows[i]).map(function (cell) {
                return '<td>' + inline(cell) + '</td>';
            }).join('') + '</tr>';
        }
        return html + '</tbody></table>';
    }

    /**
     * 把 Markdown 文本渲染为 HTML。
     * 支持：标题、有序/无序列表、代码块、行内代码、引用、表格、分割线、加粗斜体、链接。
     */
    function renderMarkdown(text) {
        if (text === null || text === undefined || text === '') {
            return '';
        }

        // 1. 先抽出围栏代码块，避免其中的内容被当作 Markdown 处理
        var codeBlocks = [];
        var source = String(text).replace(/\r\n/g, '\n');
        source = source.replace(/```[ \t]*([A-Za-z0-9+#-]*)[ \t]*\n?([\s\S]*?)```/g,
            function (match, language, code) {
                var body = code.replace(/\n+$/, '');
                codeBlocks.push('<pre class="md-code"' + (language ? ' data-lang="' + language + '"' : '')
                    + '><code>' + escapeHtml(body) + '</code></pre>');
                return '\u0000CODE' + (codeBlocks.length - 1) + '\u0000';
            });

        // 2. 整体转义，之后再做 Markdown 转换，保证不会被注入脚本
        var lines = escapeHtml(source).split('\n');
        var out = [];
        var listType = null;
        var tableRows = null;

        function closeList() {
            if (listType) {
                out.push('</' + listType + '>');
                listType = null;
            }
        }

        function closeTable() {
            if (tableRows) {
                out.push(renderTable(tableRows));
                tableRows = null;
            }
        }

        function closeAll() {
            closeList();
            closeTable();
        }

        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            var trimmed = line.trim();

            // 代码块占位
            var block = trimmed.match(/^\u0000CODE(\d+)\u0000$/);
            if (block) {
                closeAll();
                out.push(codeBlocks[Number(block[1])]);
                continue;
            }

            // 空行结束列表与表格
            if (!trimmed) {
                closeAll();
                continue;
            }

            // 表格
            if (isTableRow(line)) {
                closeList();
                if (!tableRows) {
                    tableRows = [];
                }
                tableRows.push(line);
                continue;
            }
            closeTable();

            // 标题
            var heading = trimmed.match(/^(#{1,4})\s+(.*)$/);
            if (heading) {
                closeList();
                var level = heading[1].length;
                out.push('<h' + level + '>' + inline(heading[2]) + '</h' + level + '>');
                continue;
            }

            // 分割线
            if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
                closeList();
                out.push('<hr>');
                continue;
            }

            // 引用（转义后 > 变为 &gt;）
            var quote = trimmed.match(/^&gt;\s?(.*)$/);
            if (quote) {
                closeList();
                out.push('<blockquote>' + inline(quote[1]) + '</blockquote>');
                continue;
            }

            // 无序列表
            var unordered = trimmed.match(/^[-*+]\s+(.*)$/);
            if (unordered) {
                if (listType !== 'ul') {
                    closeList();
                    out.push('<ul>');
                    listType = 'ul';
                }
                out.push('<li>' + inline(unordered[1]) + '</li>');
                continue;
            }

            // 有序列表
            var ordered = trimmed.match(/^\d+[.)]\s+(.*)$/);
            if (ordered) {
                if (listType !== 'ol') {
                    closeList();
                    out.push('<ol>');
                    listType = 'ol';
                }
                out.push('<li>' + inline(ordered[1]) + '</li>');
                continue;
            }

            closeList();
            out.push('<p>' + inline(trimmed) + '</p>');
        }

        closeAll();
        return out.join('');
    }

    global.Markdown = {
        render: renderMarkdown,
        escape: escapeHtml
    };
})(window);
