const state = {
    conversationId: null,
    waiting: false
};

const elements = {
    messages: document.getElementById('messages'),
    input: document.getElementById('input'),
    send: document.getElementById('send'),
    composer: document.getElementById('composer'),
    newChat: document.getElementById('newChat'),
    conversationList: document.getElementById('conversationList'),
    toolList: document.getElementById('toolList'),
    modelInfo: document.getElementById('modelInfo'),
    sessionTag: document.getElementById('sessionTag')
};

function appendMessage(role, text, trace) {
    const wrapper = document.createElement('div');
    wrapper.className = 'message ' + role;
    const avatar = document.createElement('div');
    avatar.className = 'avatar';
    avatar.textContent = role === 'user' ? '我' : 'A';
    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    // 助手回复通常是 Markdown，渲染后再插入；用户输入保持纯文本，避免误解析
    if (role === 'bot') {
        bubble.innerHTML = Markdown.render(text);
    } else {
        bubble.textContent = text;
    }
    if (trace && trace.length > 0) {
        const details = document.createElement('details');
        details.className = 'trace';
        const summary = document.createElement('summary');
        summary.textContent = '查看工具调用过程（' + trace.length + ' 步）';
        details.appendChild(summary);
        const body = document.createElement('div');
        body.textContent = trace.join('\n');
        details.appendChild(body);
        bubble.appendChild(details);
    }
    wrapper.appendChild(avatar);
    wrapper.appendChild(bubble);
    elements.messages.appendChild(wrapper);
    elements.messages.scrollTop = elements.messages.scrollHeight;
    return bubble;
}

function setWaiting(waiting) {
    state.waiting = waiting;
    elements.send.disabled = waiting;
    elements.send.textContent = waiting ? '处理中…' : '发送';
}

async function sendMessage(text) {
    if (!text || state.waiting) {
        return;
    }
    appendMessage('user', text);
    elements.input.value = '';
    setWaiting(true);
    const thinking = appendMessage('bot', '正在处理…');
    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({conversationId: state.conversationId, message: text})
        });
        const data = await response.json();
        state.conversationId = data.conversationId;
        localStorage.setItem('agentConversationId', data.conversationId);
        elements.sessionTag.textContent = '会话 ' + data.conversationId;
        thinking.innerHTML = Markdown.render(data.reply);
        if (data.trace && data.trace.length > 0) {
            const details = document.createElement('details');
            details.className = 'trace';
            const summary = document.createElement('summary');
            summary.textContent = '查看工具调用过程（' + data.trace.length + ' 步）';
            details.appendChild(summary);
            const body = document.createElement('div');
            body.textContent = data.trace.join('\n');
            details.appendChild(body);
            thinking.appendChild(details);
        }
        loadConversations();
    } catch (error) {
        // 报错信息里常含 URL 与尖括号，用纯文本插入避免被误解析
        thinking.textContent = '请求失败：' + error.message;
    } finally {
        setWaiting(false);
        elements.input.focus();
    }
}

async function loadTools() {
    try {
        const response = await fetch('/api/tools');
        const data = await response.json();
        elements.modelInfo.textContent = data.model;
        elements.toolList.innerHTML = '';
        data.tools.forEach(tool => {
            const item = document.createElement('li');
            item.textContent = tool.name + ' · ' + tool.description;
            item.title = tool.description;
            elements.toolList.appendChild(item);
        });
    } catch (error) {
        elements.modelInfo.textContent = '服务未连接';
    }
}

async function loadConversations() {
    try {
        const response = await fetch('/api/conversations');
        const data = await response.json();
        elements.conversationList.innerHTML = '';
        data.conversations.forEach(id => {
            const item = document.createElement('li');
            item.textContent = '会话 ' + id;
            if (id === state.conversationId) {
                item.className = 'active';
            }
            item.onclick = () => switchConversation(id);
            elements.conversationList.appendChild(item);
        });
    } catch (error) {
        // 会话列表加载失败不影响聊天
    }
}

async function switchConversation(id) {
    state.conversationId = id;
    localStorage.setItem('agentConversationId', id);
    elements.sessionTag.textContent = '会话 ' + id;
    elements.messages.innerHTML = '';
    appendMessage('bot', '已切换到历史会话 ' + id + '，继续提问即可。');
    loadConversations();
}

function newConversation() {
    state.conversationId = null;
    localStorage.removeItem('agentConversationId');
    elements.sessionTag.textContent = '新会话';
    elements.messages.innerHTML = '';
    appendMessage('bot', '新的会话已开启，我可以帮你查天气、算数据、写文件、管待办。');
    loadConversations();
}

elements.composer.addEventListener('submit', event => {
    event.preventDefault();
    sendMessage(elements.input.value.trim());
});

elements.newChat.addEventListener('click', newConversation);

document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => sendMessage(chip.dataset.example));
});

state.conversationId = localStorage.getItem('agentConversationId');
elements.sessionTag.textContent = state.conversationId ? '会话 ' + state.conversationId : '新会话';
loadTools();
loadConversations();
elements.input.focus();
