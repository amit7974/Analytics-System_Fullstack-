/* ═══════════════════════════════════════════════════
   AnalytIQ — Frontend Application Logic
   Talks to Spring Boot backend at /api/*
   ═══════════════════════════════════════════════════ */

const API = '';   // same origin — Spring Boot serves both

// ── Local counters (lightweight tracking) ─────────────
let localSearchCount = 0;
let localDocCount    = 0;

/* ══════════════════════════════════════════════════════
   NAVIGATION
   ══════════════════════════════════════════════════════ */
const PAGE_TITLES = {
  overview:  'Overview',
  users:     'Users',
  events:    'Track Events',
  analytics: 'Analytics',
  search:    'Semantic Search',
};

function navigate(page) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

  const pageEl = document.getElementById('page-' + page);
  const navEl  = document.querySelector(`[data-page="${page}"]`);
  if (pageEl) pageEl.classList.add('active');
  if (navEl)  navEl.classList.add('active');

  document.getElementById('page-title').textContent = PAGE_TITLES[page] || page;

  // auto-load data for each page
  if (page === 'overview')  loadOverview();
  if (page === 'users')     loadUsers();
}

function refreshCurrentPage() {
  const active = document.querySelector('.nav-item.active');
  if (active) navigate(active.dataset.page);
}

function toggleSidebar() {
  const sb   = document.getElementById('sidebar');
  const main = document.querySelector('.main');
  if (window.innerWidth <= 768) {
    sb.classList.toggle('open');
  } else {
    sb.classList.toggle('collapsed');
    main.classList.toggle('expanded');
  }
}

/* ══════════════════════════════════════════════════════
   HTTP HELPERS
   ══════════════════════════════════════════════════════ */
async function apiFetch(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try { const j = await res.json(); msg = j.message || j.error || JSON.stringify(j); } catch (_) {}
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

/* ══════════════════════════════════════════════════════
   TOAST
   ══════════════════════════════════════════════════════ */
let toastTimer;
function showToast(msg, type = 'info') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className   = `toast ${type} show`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 3500);
}

/* ══════════════════════════════════════════════════════
   LOADER
   ══════════════════════════════════════════════════════ */
function showLoader() { document.getElementById('loader').classList.remove('hidden'); }
function hideLoader() { document.getElementById('loader').classList.add('hidden'); }

/* ══════════════════════════════════════════════════════
   MESSAGE BOXES
   ══════════════════════════════════════════════════════ */
function setMsg(id, text, type) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text;
  el.className   = 'msg-box ' + type;
}
function clearMsg(id) { setMsg(id, '', ''); }

/* ══════════════════════════════════════════════════════
   TIME
   ══════════════════════════════════════════════════════ */
function updateTime() {
  const el = document.getElementById('time-badge');
  if (el) el.textContent = new Date().toLocaleTimeString();
}

/* ══════════════════════════════════════════════════════
   OVERVIEW
   ══════════════════════════════════════════════════════ */
async function loadOverview() {
  try {
    // Fetch users
    const users = await apiFetch('/api/users');
    document.getElementById('stat-users').textContent = users.length;

    // stat placeholders from local counters
    document.getElementById('stat-docs').textContent    = localDocCount;
    document.getElementById('stat-searches').textContent = localSearchCount;

    // try aggregate to get event count
    try {
      const agg = await apiFetch('/api/analytics/events/aggregate');
      document.getElementById('stat-events').textContent = agg.totalEvents ?? '—';

      // render bar chart
      renderBarChart('event-type-chart', agg.eventsByType || {});
    } catch (_) {
      document.getElementById('stat-events').textContent = '—';
      document.getElementById('event-type-chart').innerHTML =
        '<div class="empty-state">Track some events to see data here!</div>';
    }

    // render recent users (last 5)
    renderRecentUsers(users.slice(-5).reverse());
  } catch (err) {
    console.warn('Overview load error:', err);
  }
}

function renderBarChart(containerId, data) {
  const container = document.getElementById(containerId);
  const entries   = Object.entries(data);
  if (!entries.length) {
    container.innerHTML = '<div class="empty-state">No events yet!</div>';
    return;
  }
  const max = Math.max(...entries.map(([,v]) => v));
  container.innerHTML = entries.map(([k, v]) => `
    <div class="bar-row">
      <span class="bar-label" title="${k}">${k}</span>
      <div class="bar-track">
        <div class="bar-fill" style="width:${max ? (v/max*100) : 0}%"></div>
      </div>
      <span class="bar-count">${v}</span>
    </div>
  `).join('');
}

function renderRecentUsers(users) {
  const el = document.getElementById('recent-users-list');
  if (!users.length) {
    el.innerHTML = '<div class="empty-state">No users yet!</div>';
    return;
  }
  el.innerHTML = users.map(u => `
    <div class="user-row">
      <div class="user-avatar">${(u.username||'?')[0].toUpperCase()}</div>
      <div class="user-meta">
        <div class="user-name">${esc(u.username)}</div>
        <div class="user-email">${u.email ? esc(u.email) : '<span style="color:var(--text-dim)">No email</span>'}</div>
      </div>
      <span class="user-id-badge">#${u.id}</span>
    </div>
  `).join('');
}

/* ══════════════════════════════════════════════════════
   USERS
   ══════════════════════════════════════════════════════ */
async function loadUsers() {
  showLoader();
  try {
    const users = await apiFetch('/api/users');
    const tbody = document.getElementById('user-tbody');
    const empty = document.getElementById('users-empty');
    if (!users.length) {
      tbody.innerHTML = '';
      empty.classList.remove('hidden');
      return;
    }
    empty.classList.add('hidden');
    tbody.innerHTML = users.map(u => `
      <tr>
        <td><span class="user-id-badge">#${u.id}</span></td>
        <td>
          <div style="display:flex;align-items:center;gap:8px">
            <div class="user-avatar" style="width:28px;height:28px;font-size:12px">${(u.username||'?')[0].toUpperCase()}</div>
            ${esc(u.username)}
          </div>
        </td>
        <td>${u.email ? esc(u.email) : '<span style="color:var(--text-dim)">—</span>'}</td>
        <td style="color:var(--text-muted);font-size:12px">${u.createdAt ? formatDate(u.createdAt) : '—'}</td>
        <td>
          <button class="action-btn" onclick="viewUserAnalytics(${u.id})">📈 Analytics</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    showToast('Failed to load users: ' + err.message, 'error');
  } finally {
    hideLoader();
  }
}

function viewUserAnalytics(userId) {
  navigate('analytics');
  document.getElementById('ua-userid').value = userId;
  fetchUserAnalytics(userId);
}

async function createUser(e) {
  e.preventDefault();
  clearMsg('create-user-msg');
  const username = document.getElementById('new-username').value.trim();
  const email    = document.getElementById('new-email').value.trim();
  if (!username) return;

  showLoader();
  try {
    const user = await apiFetch('/api/users', {
      method: 'POST',
      body: JSON.stringify({ username, email: email || null }),
    });
    setMsg('create-user-msg', `✅ User "${user.username}" created with ID #${user.id}`, 'success');
    showToast(`User "${user.username}" created!`, 'success');
    document.getElementById('new-username').value = '';
    document.getElementById('new-email').value    = '';
    loadUsers();
  } catch (err) {
    setMsg('create-user-msg', '❌ ' + err.message, 'error');
    showToast(err.message, 'error');
  } finally {
    hideLoader();
  }
}

/* ══════════════════════════════════════════════════════
   EVENTS
   ══════════════════════════════════════════════════════ */
function fillPreset(type, metaJson) {
  document.getElementById('ev-type').value     = type;
  document.getElementById('ev-metadata').value = metaJson.replace(/&quot;/g, '"');
}

async function trackEvent(e) {
  e.preventDefault();
  clearMsg('track-event-msg');
  const userId    = parseInt(document.getElementById('ev-userid').value);
  const eventType = document.getElementById('ev-type').value.trim();
  const metaStr   = document.getElementById('ev-metadata').value.trim();

  let metadata = null;
  if (metaStr) {
    try { metadata = JSON.parse(metaStr); }
    catch (_) {
      setMsg('track-event-msg', '❌ Metadata must be valid JSON', 'error');
      return;
    }
  }

  showLoader();
  try {
    const ev = await apiFetch('/api/events', {
      method: 'POST',
      body: JSON.stringify({ userId, eventType, metadata }),
    });
    setMsg('track-event-msg', `✅ Event "${ev.eventType}" tracked (ID #${ev.id}) for user #${ev.userId}`, 'success');
    showToast(`Event "${eventType}" tracked!`, 'success');
    document.getElementById('ev-metadata').value = '';
  } catch (err) {
    setMsg('track-event-msg', '❌ ' + err.message, 'error');
    showToast(err.message, 'error');
  } finally {
    hideLoader();
  }
}

/* ══════════════════════════════════════════════════════
   ANALYTICS
   ══════════════════════════════════════════════════════ */
async function loadUserAnalytics(e) {
  e.preventDefault();
  const id = parseInt(document.getElementById('ua-userid').value);
  await fetchUserAnalytics(id);
}

async function fetchUserAnalytics(userId) {
  const el = document.getElementById('user-analytics-result');
  el.innerHTML = '<div style="color:var(--text-muted);font-size:13px">Loading…</div>';
  try {
    const data = await apiFetch(`/api/analytics/users/${userId}`);
    el.innerHTML = `
      ${kv('User ID',       '#' + data.userId)}
      ${kv('Total Events',  data.totalEvents)}
      ${kv('First Event',   data.firstEventAt ? formatDate(data.firstEventAt) : '—')}
      ${kv('Last Event',    data.lastEventAt  ? formatDate(data.lastEventAt)  : '—')}
      ${mapBlock('Events by Type', data.eventsByType)}
    `;
  } catch (err) {
    el.innerHTML = `<div class="msg-box error">❌ ${esc(err.message)}</div>`;
  }
}

async function loadAggregate(e) {
  e.preventDefault();
  const el    = document.getElementById('aggregate-result');
  const fromV = document.getElementById('agg-from').value;
  const toV   = document.getElementById('agg-to').value;

  let url = '/api/analytics/events/aggregate';
  const params = [];
  if (fromV) params.push('from=' + encodeURIComponent(new Date(fromV).toISOString()));
  if (toV)   params.push('to='   + encodeURIComponent(new Date(toV).toISOString()));
  if (params.length) url += '?' + params.join('&');

  el.innerHTML = '<div style="color:var(--text-muted);font-size:13px">Loading…</div>';
  try {
    const data = await apiFetch(url);
    el.innerHTML = `
      ${kv('Total Events',  data.totalEvents)}
      ${kv('Period From',   data.from ? formatDate(data.from) : 'All time')}
      ${kv('Period To',     data.to   ? formatDate(data.to)   : 'All time')}
      ${mapBlock('Events by Type', data.eventsByType)}
      ${mapBlock('Events by User', data.eventsByUser)}
    `;
  } catch (err) {
    el.innerHTML = `<div class="msg-box error">❌ ${esc(err.message)}</div>`;
  }
}

function kv(key, val) {
  return `<div class="analytics-kv"><span class="key">${key}</span><span class="val">${val}</span></div>`;
}

function mapBlock(title, obj) {
  if (!obj || !Object.keys(obj).length) return '';
  const rows = Object.entries(obj)
    .map(([k, v]) => `<div class="map-entry"><span class="mk">${esc(String(k))}</span><span class="mv">${v}</span></div>`)
    .join('');
  return `
    <div style="margin-top:6px">
      <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px;font-weight:600;text-transform:uppercase;letter-spacing:.5px">${title}</div>
      <div class="map-entries">${rows}</div>
    </div>`;
}

/* ══════════════════════════════════════════════════════
   SEMANTIC SEARCH
   ══════════════════════════════════════════════════════ */
async function indexDocument(e) {
  e.preventDefault();
  clearMsg('index-msg');
  const content = document.getElementById('doc-content').value.trim();
  if (!content) return;

  showLoader();
  try {
    const result = await apiFetch('/api/search/index', {
      method: 'POST',
      body: JSON.stringify({ content }),
    });
    localDocCount++;
    setMsg('index-msg', '✅ Document indexed successfully!', 'success');
    showToast('Document indexed!', 'success');
    document.getElementById('doc-content').value = '';
    document.getElementById('indexed-result').innerHTML = `
      <div class="search-result-card">
        <div class="result-header">
          <span class="result-rank">✓</span>
          <span class="result-score">Score: ${(result.score ?? 1).toFixed(4)}</span>
        </div>
        <div class="result-content">${esc(result.content || content)}</div>
      </div>`;
  } catch (err) {
    setMsg('index-msg', '❌ ' + err.message, 'error');
    showToast(err.message, 'error');
  } finally {
    hideLoader();
  }
}

async function semanticSearch(e) {
  e.preventDefault();
  const query  = document.getElementById('search-query').value.trim();
  const topK   = parseInt(document.getElementById('search-topk').value) || 5;
  const el     = document.getElementById('search-results');
  if (!query) return;

  el.innerHTML = '<div style="color:var(--text-muted);font-size:13px;padding:12px">Searching…</div>';
  showLoader();
  try {
    const results = await apiFetch('/api/search/semantic', {
      method: 'POST',
      body: JSON.stringify({ query, topK }),
    });
    localSearchCount++;
    if (!results.length) {
      el.innerHTML = '<div class="empty-state">No results found. Index some documents first!</div>';
      return;
    }
    el.innerHTML = results.map((r, i) => `
      <div class="search-result-card">
        <div class="result-header">
          <span class="result-rank">${i + 1}</span>
          <span class="result-score">Score: ${(r.score ?? 0).toFixed(4)}</span>
        </div>
        <div class="result-content">${esc(r.content || '—')}</div>
      </div>`).join('');
    showToast(`Found ${results.length} result${results.length !== 1 ? 's' : ''}`, 'success');
  } catch (err) {
    el.innerHTML = `<div class="msg-box error">❌ ${esc(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    hideLoader();
  }
}

/* ══════════════════════════════════════════════════════
   UTILITIES
   ══════════════════════════════════════════════════════ */
function esc(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function formatDate(iso) {
  try {
    return new Date(iso).toLocaleString(undefined, {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch (_) { return iso; }
}

/* ══════════════════════════════════════════════════════
   INIT
   ══════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  updateTime();
  setInterval(updateTime, 1000);
  navigate('overview');
});
