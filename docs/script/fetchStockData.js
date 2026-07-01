// ==UserScript==
// @name         东方财富行情表当前页转 JSON
// @namespace    holdlens.eastmoney.quote
// @version      0.2.0
// @description  读取东方财富行情中心 quotetable 当前页数据并展示为 JSON，支持模拟点击下一页
// @match        https://quote.eastmoney.com/center/gridlist.html*
// @run-at       document-idle
// @grant        none
// ==/UserScript==

(function () {
    'use strict';

    const TARGET_FIELDS = [
        '序号',
        '代码',
        '名称',
        '最新价',
        '涨跌幅',
        '涨跌额',
        '成交量(手)',
        '成交额',
        '振幅',
        '最高',
        '最低',
        '今开',
        '昨收',
        '量比',
        '换手率',
        '市盈率(动态)',
        '市净率'
    ];

    const normalize = (text) => (text || '').replace(/\s+/g, '').trim();

    function waitForTable(timeout = 15000) {
        return new Promise((resolve, reject) => {
            const existed = document.querySelector('.quotetable table');
            if (existed) {
                resolve(existed);
                return;
            }

            const timer = setTimeout(() => {
                observer.disconnect();
                reject(new Error('未找到 .quotetable table，请确认行情表格已加载'));
            }, timeout);

            const observer = new MutationObserver(() => {
                const table = document.querySelector('.quotetable table');
                if (table) {
                    clearTimeout(timer);
                    observer.disconnect();
                    resolve(table);
                }
            });

            observer.observe(document.body, { childList: true, subtree: true });
        });
    }

    function collectQuoteTableJson() {
        const table = document.querySelector('.quotetable table');
        if (!table) {
            throw new Error('未找到 .quotetable table');
        }

        const headers = Array.from(table.querySelectorAll('thead th'))
            .map((th) => normalize(th.innerText || th.textContent));

        const columnIndexes = TARGET_FIELDS.map((field) => ({
            field,
            index: headers.findIndex((header) => header === normalize(field))
        })).filter((item) => item.index >= 0);

        const rows = Array.from(table.querySelectorAll('tbody tr'));

        return rows.map((tr) => {
            const cells = Array.from(tr.querySelectorAll('td'));
            const item = {};

            columnIndexes.forEach(({ field, index }) => {
                item[field] = normalize(cells[index]?.innerText || cells[index]?.textContent);
            });

            return item;
        }).filter((item) => Object.keys(item).length > 0);
    }

    function showJson(data) {
        const oldPanel = document.querySelector('#eastmoney-json-panel');
        if (oldPanel) oldPanel.remove();

        const panel = document.createElement('div');
        panel.id = 'eastmoney-json-panel';
        panel.style.cssText = `
      position: fixed;
      right: 16px;
      bottom: 16px;
      z-index: 999999;
      width: min(760px, calc(100vw - 32px));
      height: min(620px, calc(100vh - 96px));
      background: #111827;
      color: #e5e7eb;
      border: 1px solid #374151;
      box-shadow: 0 16px 40px rgba(0,0,0,.28);
      font-size: 13px;
      display: flex;
      flex-direction: column;
    `;

        panel.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-bottom:1px solid #374151;">
        <strong>quotetable JSON，共 ${data.length} 条</strong>
        <button id="eastmoney-json-close" style="cursor:pointer;border:0;background:#374151;color:#fff;padding:4px 10px;">关闭</button>
      </div>
      <pre style="flex:1;margin:0;padding:12px;overflow:auto;white-space:pre-wrap;word-break:break-word;"></pre>
    `;

        panel.querySelector('pre').textContent = JSON.stringify(data, null, 2);
        panel.querySelector('#eastmoney-json-close').onclick = () => panel.remove();
        document.body.appendChild(panel);
    }

    function findNextPageLink() {
        const pager = document.querySelector('.qtpager');
        if (!pager) return null;

        const links = Array.from(pager.querySelectorAll('a'));
        return links.find((a) => {
            const text = normalize(a.innerText || a.textContent);
            const title = normalize(a.title || a.getAttribute('aria-label') || '');
            return text === '>' || title.includes('下一页');
        });
    }

    function clickNextPage() {
        const next = findNextPageLink();
        if (!next) {
            alert('未找到“下一页”按钮');
            return;
        }

        ['mouseover', 'mousedown', 'mouseup', 'click'].forEach((type) => {
            next.dispatchEvent(new MouseEvent(type, {
                bubbles: true,
                cancelable: true,
                view: window
            }));
        });
    }

    async function createButtons() {
        await waitForTable();

        if (document.querySelector('#eastmoney-json-toolbar')) return;

        const toolbar = document.createElement('div');
        toolbar.id = 'eastmoney-json-toolbar';
        toolbar.style.cssText = `
      position: fixed;
      right: 16px;
      top: 120px;
      z-index: 999999;
      display: flex;
      gap: 8px;
      align-items: center;
    `;

        const exportButton = document.createElement('button');
        exportButton.textContent = '导出当前页 JSON';
        exportButton.style.cssText = `
      padding: 8px 12px;
      border: 0;
      background: #2563eb;
      color: #fff;
      cursor: pointer;
      font-size: 14px;
      box-shadow: 0 8px 20px rgba(0,0,0,.18);
    `;
        exportButton.onclick = () => {
            try {
                showJson(collectQuoteTableJson());
            } catch (error) {
                alert(error.message);
            }
        };

        const nextButton = document.createElement('button');
        nextButton.textContent = '下一页';
        nextButton.style.cssText = `
      padding: 8px 12px;
      border: 0;
      background: #16a34a;
      color: #fff;
      cursor: pointer;
      font-size: 14px;
      box-shadow: 0 8px 20px rgba(0,0,0,.18);
    `;
        nextButton.onclick = clickNextPage;

        toolbar.appendChild(exportButton);
        toolbar.appendChild(nextButton);
        document.body.appendChild(toolbar);
    }

    createButtons().catch((error) => {
        console.warn('[东方财富 JSON]', error);
    });
})();