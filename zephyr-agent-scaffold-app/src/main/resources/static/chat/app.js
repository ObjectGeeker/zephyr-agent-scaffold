(function () {
    'use strict';

    const API_ROOT = '../agent';
    const STORAGE_KEY = 'zephyr-chat-settings';
    const CONVERSATIONS_KEY = 'zephyr-chat-conversations';
    const SESSION_STORAGE_KEY = 'zephyr-chat-session-id';
    const state = {
        settings: loadSettings(),
        agents: [],
        messages: [],
        conversationTitle: '',
        isSending: false,
        sessionId: localStorage.getItem(SESSION_STORAGE_KEY) || '',
        userId: localStorage.getItem('zephyr-user-id') || createId()
    };
    localStorage.setItem('zephyr-user-id', state.userId);

    const els = {
        messages: document.getElementById('messages'), welcome: document.getElementById('welcomeView'),
        input: document.getElementById('messageInput'), form: document.getElementById('composerForm'),
        send: document.getElementById('sendButton'), model: document.getElementById('modelSelect'),
        agent: document.getElementById('agentSelect'),
        conversations: document.getElementById('conversationList'), conversationCount: document.getElementById('conversationCount'), modal: document.getElementById('settingsModal'),
        baseUrl: document.getElementById('baseUrlInput'), apiKey: document.getElementById('apiKeyInput'),
        modelList: document.getElementById('modelListInput'), toast: document.getElementById('toast'),
        connectionText: document.getElementById('connectionText'), statusDot: document.querySelector('.status-dot'),
        sidebar: document.getElementById('sidebar'), sidebarBackdrop: document.getElementById('sidebarBackdrop'), typingRow: document.getElementById('typingRow')
    };

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        bindEvents();
        renderSettings();
        renderConversations();
        loadAgents().then(function () { return ensureSession(); });
        autoResize();
    }

    function bindEvents() {
        els.form.addEventListener('submit', function (event) { event.preventDefault(); sendMessage(); });
        els.input.addEventListener('input', autoResize);
        els.input.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); sendMessage(); }
        });
        document.querySelectorAll('.prompt-card').forEach(function (button) {
            button.addEventListener('click', function () { els.input.value = button.dataset.prompt; autoResize(); els.input.focus(); });
        });
        document.getElementById('newChatButton').addEventListener('click', startNewChat);
        document.getElementById('settingsButton').addEventListener('click', openSettings);
        document.getElementById('topSettingsButton').addEventListener('click', openSettings);
        document.getElementById('closeSettingsButton').addEventListener('click', closeSettings);
        document.getElementById('saveSettingsButton').addEventListener('click', saveSettings);
        document.getElementById('resetSettingsButton').addEventListener('click', resetSettings);
        document.getElementById('toggleKeyButton').addEventListener('click', toggleApiKeyVisibility);
        document.getElementById('mobileMenuButton').addEventListener('click', function () { setSidebarOpen(!els.sidebar.classList.contains('open')); });
        els.sidebarBackdrop.addEventListener('click', function () { setSidebarOpen(false); });
        els.modal.addEventListener('click', function (event) { if (event.target === els.modal) closeSettings(); });
        document.addEventListener('keydown', function (event) {
            if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); startNewChat(); }
            if (event.key === 'Escape') { closeSettings(); setSidebarOpen(false); }
        });
        els.model.addEventListener('change', function () { state.settings.model = els.model.value; persistSettings(); });
    }

    async function loadAgents() {
        setConnection('正在连接服务', 'pending');
        try {
            const response = await fetch(API_ROOT + '/query_ai_agent_list', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
            if (!response.ok) throw new Error('HTTP ' + response.status);
            state.agents = await response.json();
            renderAgents();
            setConnection('服务已连接', 'online');
        } catch (error) {
            state.agents = [{ agentId: 'agent-001', agentName: 'daily-assistant', agentDesc: '日常聊天助手' }];
            renderAgents();
            setConnection('服务暂不可用', 'offline');
        }
    }

    async function sendMessage() {
        const message = els.input.value.trim();
        if (!message || state.isSending) return;

        const agent = getSelectedAgent();
        if (!state.sessionId) {
            try {
                await ensureSession(agent.agentId);
            } catch (error) {
                showToast('无法创建对话，请检查服务是否启动');
                return;
            }
        }

        state.isSending = true;
        if (!state.conversationTitle) state.conversationTitle = message;
        els.input.value = '';
        autoResize();
        hideWelcome();
        appendUserMessage(message);
        showTyping(true);
        els.send.disabled = true;
        const payload = {
            agentId: agent.agentId,
            sessionId: state.sessionId,
            userId: state.userId,
            baseUrl: state.settings.baseUrl || null,
            apiKey: state.settings.apiKey || null,
            model: els.model.value || null,
            texts: [{ message: message }],
            files: [],
            inlineDatas: []
        };
        const streamState = { currentText: null, currentTool: null, hasVisibleEvent: false };
        try {
            const response = await fetch(API_ROOT + '/stream', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            if (!response.ok) throw new Error('HTTP ' + response.status);
            await consumeSse(response, streamState);
            if (!state.messages.some(function (item) { return item.type === 'assistant' && item.text.trim(); }) && !streamState.hasVisibleEvent) {
                appendAssistantContent(streamState, '接口已返回，但没有可展示的内容。');
            }
            persistConversationSafely();
        } catch (error) {
            console.error('Zephyr stream completed with a client-side error:', error);
            if (!streamState.hasVisibleEvent) {
                const errorMessage = '暂时无法连接到智能体服务。请检查服务是否启动，或在设置中确认 Base URL 和 API Key。';
                appendAssistantContent(streamState, errorMessage);
                showToast('消息发送失败，请检查服务配置');
            } else {
                showToast('回答已接收');
            }
            persistConversationSafely();
        } finally {
            showTyping(false);
            state.isSending = false;
            els.send.disabled = false;
            els.input.focus();
        }
    }

    async function ensureSession(agentId) {
        if (state.sessionId) return state.sessionId;
        const agent = agentId ? state.agents.find(function (item) { return item.agentId === agentId; }) || getSelectedAgent() : getSelectedAgent();
        const response = await fetch(API_ROOT + '/session/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ agentId: agent.agentId, userId: state.userId })
        });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const result = await response.json();
        if (!result.sessionId) throw new Error('sessionId is missing');
        state.sessionId = result.sessionId;
        localStorage.setItem(SESSION_STORAGE_KEY, state.sessionId);
        return state.sessionId;
    }

    function getSelectedAgent() {
        return state.agents.find(function (item) { return item.agentId === els.agent.value; })
            || state.agents[0]
            || { agentId: 'agent-001' };
    }

    async function consumeSse(response, streamState) {
        if (!response.body) throw new Error('SSE response body is unavailable');
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        while (true) {
            const result = await reader.read();
            if (result.value) buffer += decoder.decode(result.value, { stream: true });
            const frames = buffer.split(/\r?\n\r?\n/);
            buffer = frames.pop() || '';
            frames.forEach(function (frame) { consumeSseFrame(frame, streamState); });
            if (result.done) {
                buffer += decoder.decode();
                if (buffer.trim()) consumeSseFrame(buffer, streamState);
                break;
            }
        }
    }

    function consumeSseFrame(frame, streamState) {
        const data = frame.split(/\r?\n/)
            .filter(function (line) { return line.startsWith('data:'); })
            .map(function (line) { return line.slice(5).trimStart(); })
            .join('\n');
        if (!data || data === '[DONE]') return;
        let payload;
        try { payload = JSON.parse(data); } catch (error) { return; }
        if (typeof payload.content === 'string' && payload.content) {
            streamState.hasVisibleEvent = true;
            appendAssistantContent(streamState, payload.content);
        }
        if (payload.toolCallName) {
            streamState.hasVisibleEvent = true;
            appendToolCall(streamState, payload.toolCallName);
        }
        if (payload.toolCallResponse) {
            streamState.hasVisibleEvent = true;
            appendToolResponse(streamState, payload.toolCallResponse);
        }
        scrollToBottom();
    }

    function appendUserMessage(text) {
        const item = { id: createId(), type: 'user', text: text };
        state.messages.push(item);
        item.element = renderUserMessage(item);
        els.messages.appendChild(item.element);
        scrollToBottom();
    }

    function appendAssistantContent(streamState, text) {
        let item = streamState.currentText;
        if (!item || state.messages[state.messages.length - 1] !== item) {
            item = { id: createId(), type: 'assistant', text: '' };
            state.messages.push(item);
            item.element = renderAssistantMessage(item);
            els.messages.appendChild(item.element);
            streamState.currentText = item;
        }
        item.text += text;
        updateAssistantMessage(item);
        scrollToBottom();
    }

    function appendToolCall(streamState, name) {
        const item = { id: createId(), type: 'tool', name: String(name), request: String(name), response: '', status: 'running' };
        state.messages.push(item);
        item.element = renderToolCard(item);
        els.messages.appendChild(item.element);
        streamState.currentTool = item;
        streamState.currentText = null;
        scrollToBottom();
    }

    function appendToolResponse(streamState, response) {
        let item = streamState.currentTool;
        if (!item) {
            item = { id: createId(), type: 'tool', name: '工具返回', request: '', response: '', status: 'running' };
            state.messages.push(item);
            item.element = renderToolCard(item);
            els.messages.appendChild(item.element);
            streamState.currentTool = item;
        }
        item.response = item.response ? item.response + '\n' + String(response) : String(response);
        item.status = 'complete';
        updateToolCard(item);
        streamState.currentText = null;
        scrollToBottom();
    }

    function renderUserMessage(item) {
        const article = document.createElement('article');
        article.className = 'message user';
        const body = createMessageBody(item, false);
        item.textElement.textContent = item.text;
        article.append(createAvatar('user'), body);
        return article;
    }

    function renderAssistantMessage(item) {
        const article = document.createElement('article');
        article.className = 'message assistant';
        article.append(createAvatar('assistant'), createMessageBody(item, true));
        return article;
    }

    function createAvatar(role) {
        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        if (role === 'assistant') { avatar.setAttribute('aria-label', 'Zephyr'); avatar.innerHTML = '<span class="brand-orbit" aria-hidden="true"></span>'; }
        else avatar.textContent = 'U';
        return avatar;
    }

    function createMessageBody(item, assistant) {
        const body = document.createElement('div');
        body.className = 'message-body';
        const text = document.createElement('div');
        text.className = 'message-text markdown-body';
        body.appendChild(text);
        item.textElement = text;
        if (assistant) {
            const meta = document.createElement('div');
            meta.className = 'message-meta';
            meta.textContent = 'Zephyr · 刚刚';
            body.appendChild(meta);
            const actions = document.createElement('div');
            actions.className = 'message-actions';
            const copy = document.createElement('button');
            copy.className = 'message-action';
            copy.type = 'button';
            copy.setAttribute('aria-label', '复制回答');
            copy.textContent = '复制';
            copy.addEventListener('click', function () {
                const write = navigator.clipboard ? navigator.clipboard.writeText(item.text) : Promise.reject();
                write.then(function () { copy.textContent = '已复制'; }).catch(function () { showToast('复制失败，请手动选择文本'); });
                setTimeout(function () { copy.textContent = '复制'; }, 1200);
            });
            actions.appendChild(copy);
            body.appendChild(actions);
        }
        return body;
    }

    function updateAssistantMessage(item) {
        item.textElement.innerHTML = renderMarkdown(item.text);
    }

    function renderMarkdown(source) {
        try {
            if (window.marked && window.DOMPurify) {
                const html = window.marked.parse(source, { gfm: true, breaks: true });
                return window.DOMPurify.sanitize(html, { USE_PROFILES: { html: true } });
            }
        } catch (error) {
            console.warn('Markdown rendering failed; using text fallback.', error);
        }
        return escapeHtml(source).replace(/\n/g, '<br>');
    }

    function renderToolCard(item) {
        const article = document.createElement('article');
        article.className = 'tool-timeline-item';
        const avatar = document.createElement('div');
        avatar.className = 'tool-avatar';
        avatar.innerHTML = '<span class="brand-orbit"></span>';
        const details = document.createElement('details');
        details.className = 'tool-card';
        details.open = false;
        details.setAttribute('aria-label', '工具调用：' + item.name);
        const summary = document.createElement('summary');
        const title = document.createElement('span');
        title.className = 'tool-card-title';
        const toolIcon = document.createElement('span');
        toolIcon.className = 'tool-card-icon';
        toolIcon.setAttribute('aria-hidden', 'true');
        title.append(toolIcon, document.createTextNode('工具调用'));
        const toolName = document.createElement('code');
        toolName.className = 'tool-name';
        toolName.textContent = item.name;
        const status = document.createElement('span');
        status.className = 'tool-status';
        summary.append(title, toolName, status);

        const content = document.createElement('div');
        content.className = 'tool-card-content';
        const requestLabel = document.createElement('div');
        requestLabel.className = 'tool-field-label';
        requestLabel.textContent = '调用';
        const request = document.createElement('pre');
        request.className = 'tool-value tool-request';
        const responseLabel = document.createElement('div');
        responseLabel.className = 'tool-field-label';
        responseLabel.textContent = '结果';
        const response = document.createElement('pre');
        response.className = 'tool-value tool-response';
        content.append(requestLabel, request, responseLabel, response);
        details.append(summary, content);
        article.append(avatar, details);
        item.statusElement = status;
        status.setAttribute('role', 'status');
        item.requestElement = request;
        item.responseElement = response;
        updateToolCard(item);
        return article;
    }

    function updateToolCard(item) {
        if (!item.statusElement) return;
        const isComplete = item.status === 'complete';
        item.statusElement.innerHTML = '<span class="status-mark" aria-hidden="true"></span><span>' + (isComplete ? '已完成' : '处理中') + '</span>';
        item.statusElement.className = 'tool-status ' + item.status;
        item.statusElement.setAttribute('aria-label', isComplete ? '工具已完成' : '工具处理中');
        item.requestElement.textContent = item.request || item.name;
        item.responseElement.textContent = item.response || '等待工具返回…';
        if (item.element) item.element.classList.toggle('is-complete', item.status === 'complete');
    }

    async function startNewChat() {
        state.messages = [];
        state.conversationTitle = '';
        state.sessionId = '';
        localStorage.removeItem(SESSION_STORAGE_KEY);
        els.messages.innerHTML = '';
        els.input.value = '';
        autoResize();
        els.welcome.classList.remove('hidden');
        setSidebarOpen(false);
        renderConversations();
        els.input.focus();
        try {
            await ensureSession();
        } catch (error) {
            showToast('新对话创建失败，请检查服务是否启动');
        }
    }

    function persistConversation() {
        if (!state.sessionId || !state.messages.length) return;
        const titles = readConversations();
        const existing = titles.find(function (item) { return item.sessionId === state.sessionId; });
        const title = state.conversationTitle || (state.messages.find(function (item) { return item.type === 'user'; }) || {}).text || '未命名对话';
        const snapshot = state.messages.map(function (item) {
            return { id: item.id, type: item.type, text: item.text || '', name: item.name || '', request: item.request || '', response: item.response || '', status: item.status || '' };
        });
        const saved = { sessionId: state.sessionId, title: title.length > 24 ? title.slice(0, 24) + '…' : title, messages: snapshot, updatedAt: Date.now() };
        if (existing) Object.assign(existing, saved);
        else titles.unshift(saved);
        titles.sort(function (a, b) { return (b.updatedAt || 0) - (a.updatedAt || 0); });
        localStorage.setItem(CONVERSATIONS_KEY, JSON.stringify(titles.slice(0, 8)));
        state.conversationTitle = title;
        renderConversations();
    }

    function persistConversationSafely() {
        try {
            persistConversation();
        } catch (error) {
            console.warn('Conversation snapshot was not saved locally.', error);
        }
    }

    function renderConversations() {
        const titles = readConversations();
        els.conversations.innerHTML = '';
        els.conversationCount.textContent = String(titles.length);
        if (!titles.length) {
            const empty = document.createElement('div');
            empty.className = 'conversation-empty';
            empty.textContent = '还没有历史对话';
            els.conversations.appendChild(empty);
            return;
        }
        titles.forEach(function (item) {
            const row = document.createElement('div');
            row.className = 'conversation-row' + (item.sessionId === state.sessionId ? ' active' : '');
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'conversation-item';
            button.setAttribute('aria-current', item.sessionId === state.sessionId ? 'page' : 'false');
            const icon = document.createElement('span');
            icon.className = 'icon conversation-icon';
            icon.setAttribute('aria-hidden', 'true');
            const title = document.createElement('span');
            title.className = 'conversation-title';
            title.textContent = item.title;
            button.append(icon, title);
            button.addEventListener('click', function () { restoreConversation(item); });
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'conversation-delete';
            remove.setAttribute('aria-label', '删除对话：' + item.title);
            remove.title = '删除对话';
            remove.innerHTML = '<span class="icon icon-trash" aria-hidden="true"></span>';
            remove.addEventListener('click', function (event) { event.stopPropagation(); deleteConversation(item); });
            row.append(button, remove);
            els.conversations.appendChild(row);
        });
    }

    function restoreConversation(item) {
        state.sessionId = item.sessionId;
        state.conversationTitle = item.title;
        localStorage.setItem(SESSION_STORAGE_KEY, state.sessionId);
        state.messages = (item.messages || []).map(function (saved) {
            return { id: saved.id || createId(), type: saved.type, text: saved.text || '', name: saved.name || '', request: saved.request || '', response: saved.response || '', status: saved.status || 'complete' };
        });
        renderTimeline();
        renderConversations();
        setSidebarOpen(false);
        els.input.focus();
    }

    function deleteConversation(item) {
        if (!window.confirm('确定删除“' + item.title + '”吗？删除后无法恢复。')) return;
        const remaining = readConversations().filter(function (conversation) { return conversation.sessionId !== item.sessionId; });
        localStorage.setItem(CONVERSATIONS_KEY, JSON.stringify(remaining));
        if (state.sessionId === item.sessionId) {
            state.messages = [];
            state.conversationTitle = '';
            state.sessionId = '';
            localStorage.removeItem(SESSION_STORAGE_KEY);
            els.messages.innerHTML = '';
            els.welcome.classList.remove('hidden');
        }
        renderConversations();
        showToast('对话已删除');
    }

    function renderTimeline() {
        els.messages.innerHTML = '';
        state.messages.forEach(function (item) {
            if (item.type === 'user') item.element = renderUserMessage(item);
            if (item.type === 'assistant') item.element = renderAssistantMessage(item);
            if (item.type === 'tool') item.element = renderToolCard(item);
            if (item.element) els.messages.appendChild(item.element);
        });
        state.messages.forEach(function (item) {
            if (item.type === 'assistant') updateAssistantMessage(item);
            if (item.type === 'tool') updateToolCard(item);
        });
        els.welcome.classList.toggle('hidden', state.messages.length > 0);
        scrollToBottom();
    }

    function readConversations() {
        try {
            const saved = JSON.parse(localStorage.getItem(CONVERSATIONS_KEY) || '[]');
            return Array.isArray(saved) ? saved : [];
        } catch (error) { return []; }
    }

    function renderAgents() {
        els.agent.innerHTML = '';
        state.agents.forEach(function (agent) {
            const option = document.createElement('option');
            option.value = agent.agentId;
            option.textContent = agent.agentName || agent.agentId;
            option.title = agent.agentDesc || '';
            els.agent.appendChild(option);
        });
    }

    function hideWelcome() { els.welcome.classList.add('hidden'); }
    function showTyping(show) { els.typingRow.classList.toggle('hidden', !show); if (show) scrollToBottom(); }
    function scrollToBottom() { requestAnimationFrame(function () { els.messages.parentElement.scrollTop = els.messages.parentElement.scrollHeight; }); }
    function autoResize() { els.input.style.height = 'auto'; els.input.style.height = Math.min(els.input.scrollHeight, 180) + 'px'; }
    function setSidebarOpen(open) {
        els.sidebar.classList.toggle('open', open);
        els.sidebarBackdrop.classList.toggle('hidden', !open);
        document.getElementById('mobileMenuButton').setAttribute('aria-expanded', String(open));
    }

    function openSettings() { renderSettings(); els.modal.classList.remove('hidden'); setTimeout(function () { els.baseUrl.focus(); }, 40); }
    function closeSettings() { els.modal.classList.add('hidden'); }
    function renderSettings() { els.baseUrl.value = state.settings.baseUrl; els.apiKey.value = state.settings.apiKey; els.modelList.value = state.settings.models.join('\n'); renderModels(); }
    function saveSettings() {
        state.settings.baseUrl = els.baseUrl.value.trim(); state.settings.apiKey = els.apiKey.value.trim();
        state.settings.models = els.modelList.value.split(/[\n,，]/).map(function (model) { return model.trim(); }).filter(Boolean);
        if (!state.settings.models.includes(state.settings.model)) state.settings.model = state.settings.models[0] || '';
        persistSettings(); renderModels(); closeSettings(); showToast('设置已保存');
    }
    function resetSettings() { state.settings = { baseUrl: '', apiKey: '', models: [], model: '' }; persistSettings(); renderSettings(); showToast('已恢复默认设置'); }
    function renderModels() {
        const selected = state.settings.model || '';
        els.model.innerHTML = '';
        const defaultOption = document.createElement('option'); defaultOption.value = ''; defaultOption.textContent = '服务端默认模型'; els.model.appendChild(defaultOption);
        state.settings.models.forEach(function (model) { const option = document.createElement('option'); option.value = model; option.textContent = model; els.model.appendChild(option); });
        els.model.value = selected;
    }
    function toggleApiKeyVisibility() { const button = document.getElementById('toggleKeyButton'); const visible = els.apiKey.type === 'text'; els.apiKey.type = visible ? 'password' : 'text'; button.textContent = visible ? '显示' : '隐藏'; }
    function persistSettings() { localStorage.setItem(STORAGE_KEY, JSON.stringify(state.settings)); }
    function loadSettings() { try { const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}'); return { baseUrl: saved.baseUrl || '', apiKey: saved.apiKey || '', models: Array.isArray(saved.models) ? saved.models : [], model: saved.model || '' }; } catch (_) { return { baseUrl: '', apiKey: '', models: [], model: '' }; } }
    function setConnection(text, status) { els.connectionText.textContent = text; els.statusDot.className = 'status-dot ' + (status === 'online' ? 'online' : status === 'offline' ? 'offline' : ''); }
    function showToast(message) { els.toast.textContent = message; els.toast.classList.add('show'); clearTimeout(showToast.timer); showToast.timer = setTimeout(function () { els.toast.classList.remove('show'); }, 2600); }
    function escapeHtml(value) { return String(value).replace(/[&<>"']/g, function (character) { return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' })[character]; }); }
    function createId() { return (crypto.randomUUID ? crypto.randomUUID() : Date.now() + '-' + Math.random().toString(16).slice(2)); }
})();
