/* ==========================================================
   Trainable — Local Web Dashboard JavaScript
   ========================================================== */

const API_BASE = '';

// ============================================================
// THEME
// ============================================================
function initTheme() {
  const html = document.documentElement;
  const themeToggle = document.getElementById('themeToggle');
  const saved = localStorage.getItem('theme') || 'dark';
  html.setAttribute('data-theme', saved);

  applyThemeColors();

  if (themeToggle) {
    themeToggle.addEventListener('click', () => {
      const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      html.setAttribute('data-theme', next);
      localStorage.setItem('theme', next);
      applyThemeColors();
    });
  }
}

async function applyThemeColors() {
  const data = await fetchApi('/api/theme');
  if (!data) return;

  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  const colors = isDark ? data.dark : data.light;
  if (!colors) return;

  const root = document.documentElement;
  root.style.setProperty('--md-primary', colors.primary || '#3A59D1');
  root.style.setProperty('--md-on-primary', colors.onPrimary || '#FFFFFF');
  root.style.setProperty('--md-primary-container', colors.primaryContainer || '#3A59D1');
  root.style.setProperty('--md-on-primary-container', colors.onPrimaryContainer || '#FFFFFF');
  root.style.setProperty('--md-secondary', colors.secondary || '#A9A0B9');
  root.style.setProperty('--md-on-secondary', colors.onSecondary || '#292336');
  root.style.setProperty('--md-secondary-container', colors.secondaryContainer || '#292336');
  root.style.setProperty('--md-on-secondary-container', colors.onSecondaryContainer || '#A9A0B9');
  root.style.setProperty('--md-tertiary', colors.tertiary || '#7AC6D2');
  root.style.setProperty('--md-on-tertiary', colors.onTertiary || '#00363D');
  root.style.setProperty('--md-tertiary-container', colors.tertiaryContainer || '#5AB1BF');
  root.style.setProperty('--md-on-tertiary-container', colors.onTertiaryContainer || '#00363D');
  root.style.setProperty('--md-surface', colors.surface || '#0E0E11');
  root.style.setProperty('--md-on-surface', colors.onSurface || '#E7E1EC');
  root.style.setProperty('--md-surface-variant', colors.surfaceVariant || '#141317');
  root.style.setProperty('--md-on-surface-variant', colors.onSurfaceVariant || '#ADA9B3');
  root.style.setProperty('--md-surface-container', colors.surfaceContainer || '#1A191E');
  root.style.setProperty('--md-surface-container-high', colors.surfaceContainerHigh || '#201F25');
  root.style.setProperty('--md-surface-container-highest', colors.surfaceContainerHighest || '#26252C');
  root.style.setProperty('--md-surface-container-low', colors.surfaceContainerLow || '#141317');
  root.style.setProperty('--md-outline', colors.outline || '#49474F');
  root.style.setProperty('--md-outline-variant', colors.outlineVariant || '#49474F');
  root.style.setProperty('--md-error', colors.error || '#FFB4AB');
  root.style.setProperty('--md-on-error', colors.onError || '#690005');

  // Nav blur uses the surface color
  root.style.setProperty('--nav-blur', `rgba(${hexToRgb(colors.surface || '#0E0E11')}, ${isDark ? '0.75' : '0.8'})`);
}

function hexToRgb(hex) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `${r}, ${g}, ${b}`;
}

// ============================================================
// NAVIGATION
// ============================================================
function initNav() {
  const currentPath = window.location.pathname;
  document.querySelectorAll('.nav-links a').forEach(link => {
    const href = link.getAttribute('href');
    if (href === currentPath) {
      link.classList.add('active');
    }
  });
}

// ============================================================
// API HELPERS
// ============================================================
async function fetchApi(endpoint) {
  try {
    const response = await fetch(`${API_BASE}${endpoint}`);
    if (!response.ok) {
      console.error(`HTTP ${response.status} for ${endpoint}`);
      return null;
    }
    const body = await response.json();
    if (!body.success) {
      console.error(`API error for ${endpoint}: ${body.error}`);
      return null;
    }
    return body.data;
  } catch (error) {
    console.error(`API Error [${endpoint}]:`, error);
    return null;
  }
}

// ============================================================
// FORMATTERS
// ============================================================
function formatVolume(volume) {
  if (!volume) return '0';
  if (volume >= 1_000_000) return (volume / 1_000_000).toFixed(1) + 'M';
  if (volume >= 1_000) return (volume / 1_000).toFixed(1) + 'k';
  return Math.round(volume).toString();
}

function formatDate(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleDateString('it-IT', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
}

function formatDateTime(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleString('it-IT', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

// ============================================================
// DASHBOARD
// ============================================================
async function loadDashboard() {
  loadMembership();
  loadWeeklyGoal();
  loadConsistencyCalendar();

  const sessions = await fetchApi('/api/sessions?limit=5');
  if (sessions && sessions.length > 0) {
    document.getElementById('recent-sessions').innerHTML = sessions.map(s => createSessionCard(s)).join('');
  } else {
    const el = document.getElementById('recent-sessions');
    if (el) {
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">fitness_center</div><div class="empty-state-text">Nessuna sessione trovata</div></div>';
    }
  }
}

async function loadMembership() {
  const data = await fetchApi('/api/dashboard/membership');
  const el = document.getElementById('membership-card');
  if (!el) return;

  if (!data || !data.expiryDate) {
    el.className = 'membership-card empty';
    el.innerHTML = `
      <div class="membership-card-header">
        <span class="material-symbols-outlined">credit_card</span>
        <span class="membership-card-title">Abbonamento Palestra</span>
      </div>
      <div class="membership-card-body">
        <div>
          <div class="membership-card-username">${data?.username || 'Athlete'}</div>
          <div class="membership-card-expiry">Nessuna scadenza impostata</div>
        </div>
      </div>
    `;
    return;
  }

  const now = Date.now();
  const daysLeft = Math.floor((data.expiryDate - now) / (1000 * 60 * 60 * 24));
  const isExpired = daysLeft < 0;
  const isExpiringSoon = daysLeft >= 0 && daysLeft <= 7;

  const expiryText = new Date(data.expiryDate).toLocaleDateString('it-IT', {
    day: 'numeric', month: 'short', year: 'numeric'
  });

  const badgeText = isExpired ? 'Scaduto' : (isExpiringSoon ? `Scade tra ${daysLeft}g` : `${daysLeft} giorni`);

  el.className = 'membership-card';
  el.innerHTML = `
    <div class="membership-card-header">
      <span class="material-symbols-outlined">credit_card</span>
      <span class="membership-card-title">Abbonamento Palestra</span>
    </div>
    <div class="membership-card-body">
      <div>
        <div class="membership-card-username">${data.username}</div>
        <div class="membership-card-expiry">Valido fino al ${expiryText}</div>
      </div>
      <span class="membership-card-badge">${badgeText}</span>
    </div>
    ${isExpired ? '<span class="material-symbols-outlined" style="position:absolute;top:24px;right:24px;font-size:24px;opacity:0.7;">warning</span>' : ''}
  `;
}

async function loadWeeklyGoal() {
  const data = await fetchApi('/api/dashboard/weekly-goal');
  const el = document.getElementById('weekly-goal-card');
  if (!el || !data) return;

  const total = data.workoutsThisWeek + (data.cardioWorkoutsThisWeek || 0);
  const goalMet = total >= data.weeklyGoal;
  const remaining = data.weeklyGoal - total;
  const progressPct = data.weeklyGoal > 0 ? Math.min(100, (total / data.weeklyGoal) * 100) : 0;

  el.innerHTML = `
    <div class="weekly-goal-label">OBIETTIVO SETTIMANALE</div>
    <div class="weekly-progress">
      <span class="weekly-progress-value">${total}</span>
      <span class="weekly-progress-target">/ ${data.weeklyGoal}</span>
      <span style="flex:1"></span>
      ${goalMet
        ? '<span class="weekly-goal-met"><span class="material-symbols-outlined" style="font-size:20px;">check_circle</span> Obiettivo raggiunto!</span>'
        : `<span class="weekly-remaining">${remaining} ${remaining === 1 ? 'allenamento rimasto' : 'allenamenti rimasti'}</span>`
      }
    </div>
    <div class="weekly-progress-bar">
      <div class="weekly-progress-fill" style="width:${progressPct}%"></div>
    </div>
  `;
}

// ============================================================
// CONSISTENCY CALENDAR (GitHub-style)
// ============================================================
async function loadConsistencyCalendar() {
  const data = await fetchApi('/api/analytics/consistency-calendar?weeks=14');
  const container = document.getElementById('calendar-container');
  if (!container || !data || !data.days) {
    if (container) container.innerHTML = '<div class="empty-state"><div class="empty-state-text">Nessun dato disponibile</div></div>';
    return;
  }

  const dayMap = {};
  data.days.forEach(d => { dayMap[d.date] = d; });

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayStr = today.toISOString().split('T')[0];

  // Find Monday of the first week (14 weeks ago)
  const startDate = new Date(today);
  startDate.setDate(startDate.getDate() - (data.weeks * 7) + 1);
  const dayOfWeek = startDate.getDay();
  const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  startDate.setDate(startDate.getDate() - diff);

  const monthNames = ['Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu', 'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic'];
  const dayLabels = ['', 'Lun', '', 'Mer', '', 'Ven', ''];

  // Build cells
  let cellsHtml = '';
  let monthLabels = [];
  let lastMonth = -1;

  for (let i = 0; i < data.weeks * 7; i++) {
    const date = new Date(startDate);
    date.setDate(date.getDate() + i);
    const dateStr = date.toISOString().split('T')[0];
    const dayData = dayMap[dateStr];
    const level = dayData ? dayData.level : 0;
    const count = dayData ? dayData.count : 0;
    const isToday = dateStr === todayStr;
    const isFuture = date > today;

    // Track month changes for labels
    if (date.getDate() <= 7 && date.getMonth() !== lastMonth) {
      monthLabels.push({ weekIndex: Math.floor(i / 7), label: monthNames[date.getMonth()] });
      lastMonth = date.getMonth();
    }

    const tooltip = isFuture ? '' : `${dateStr}: ${count} ${count === 1 ? 'allenamento' : 'allenamenti'}`;
    const classes = ['calendar-cell'];
    if (level > 0) classes.push('l' + level);
    if (isToday) classes.push('today');
    if (isFuture) classes.push('future');

    cellsHtml += `<div class="${classes.join(' ')}" data-tooltip="${tooltip}"></div>`;
  }

  // Build month labels
  const monthLabelsHtml = monthLabels.map(m => {
    const leftPercent = (m.weekIndex / data.weeks) * 100;
    return `<span class="calendar-month-label" style="left:${leftPercent}%">${m.label}</span>`;
  }).join('');

  // Build day labels
  const dayLabelsHtml = dayLabels.map(d => `<div class="calendar-day-label">${d}</div>`).join('');

  container.innerHTML = `
    <div class="calendar-container">
      <div class="calendar-header">
        <span class="chart-title">Ultimi ${data.weeks} settimane</span>
        <div class="calendar-legend">
          <span>Meno</span>
          <div class="legend-box l0"></div>
          <div class="legend-box l1"></div>
          <div class="legend-box l2"></div>
          <div class="legend-box l3"></div>
          <div class="legend-box l4"></div>
          <span>Più</span>
        </div>
      </div>
      <div class="calendar-layout">
        <div class="calendar-day-labels">${dayLabelsHtml}</div>
        <div class="calendar-main">
          <div class="calendar-month-labels">${monthLabelsHtml}</div>
          <div class="calendar-grid">${cellsHtml}</div>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// ANALYTICS
// ============================================================
async function loadAnalytics() {
  const [volumeHistory, personalBests, categoryVolume, strengthIndex] = await Promise.all([
    fetchApi('/api/analytics/volume?days=90'),
    fetchApi('/api/analytics/personal-bests'),
    fetchApi('/api/analytics/volume-by-category?days=30'),
    fetchApi('/api/analytics/strength-index')
  ]);

  if (strengthIndex !== null) {
    const el = document.getElementById('strength-index');
    if (el) {
      el.textContent = strengthIndex > 0 ? `+${strengthIndex.toFixed(1)}%` : `${strengthIndex.toFixed(1)}%`;
    }
  }

  if (personalBests && personalBests.length > 0) {
    const el = document.getElementById('personal-bests-table');
    if (el) {
      el.innerHTML = personalBests.slice(0, 20).map(pb => `
        <tr>
          <td>${pb.exerciseName}</td>
          <td><span class="badge badge-neutral">${pb.category}</span></td>
          <td><strong>${pb.maxWeight} kg</strong></td>
          <td>${pb.reps} reps</td>
        </tr>
      `).join('');
    }
  }

  if (categoryVolume && categoryVolume.length > 0) {
    renderCategoryChart(categoryVolume);
  }

  if (volumeHistory && volumeHistory.length > 0) {
    renderVolumeChart(volumeHistory);
  }
}

function renderVolumeChart(data) {
  const canvas = document.getElementById('volume-chart');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width = canvas.parentElement.clientWidth - 56;
  const h = canvas.height = 300;

  const maxVol = Math.max(...data.map(d => d.volume));
  const barWidth = Math.max(4, (w - data.length * 2) / data.length);

  ctx.clearRect(0, 0, w, h);

  data.forEach((d, i) => {
    const barHeight = (d.volume / maxVol) * (h - 40);
    const x = i * (barWidth + 2);
    const y = h - barHeight - 20;

    const gradient = ctx.createLinearGradient(x, y, x, h - 20);
    gradient.addColorStop(0, '#3A59D1');
    gradient.addColorStop(1, '#5AB1BF');

    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.roundRect(x, y, barWidth, barHeight, 4);
    ctx.fill();
  });
}

function renderCategoryChart(data) {
  const canvas = document.getElementById('category-chart');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width = canvas.parentElement.clientWidth - 56;
  const h = canvas.height = 300;

  const maxVol = Math.max(...data.map(d => d.volume));
  const barHeight = 32;
  const gap = 12;
  const labelWidth = 100;

  ctx.clearRect(0, 0, w, h);
  ctx.font = '14px Google Sans Flex, Outfit, sans-serif';

  data.forEach((d, i) => {
    const y = i * (barHeight + gap) + 10;
    const barW = ((w - labelWidth - 20) * d.volume) / maxVol;

    ctx.fillStyle = '#ADA9B3';
    ctx.textAlign = 'right';
    ctx.fillText(d.category, labelWidth - 12, y + barHeight / 2 + 5);

    const gradient = ctx.createLinearGradient(labelWidth, y, labelWidth + barW, y);
    gradient.addColorStop(0, '#3A59D1');
    gradient.addColorStop(1, '#7AC6D2');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.roundRect(labelWidth, y, barW, barHeight, 6);
    ctx.fill();

    ctx.fillStyle = '#E7E1EC';
    ctx.textAlign = 'left';
    ctx.fillText(Math.round(d.volume), labelWidth + barW + 8, y + barHeight / 2 + 5);
  });
}

// ============================================================
// PLANS
// ============================================================
async function loadPlans() {
  const plans = await fetchApi('/api/plans');
  if (!plans || plans.length === 0) {
    const el = document.getElementById('plans-container');
    if (el) {
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">playlist_add_check</div><div class="empty-state-text">Nessun piano trovato</div></div>';
    }
    return;
  }

  const el = document.getElementById('plans-container');
  if (el) {
    el.innerHTML = plans.map(plan => `
      <div class="plan-card">
        <div class="plan-card-header">
          <div class="plan-card-name">${plan.name}</div>
          ${plan.isActive
            ? '<span class="badge badge-success">Attivo</span>'
            : '<span class="badge badge-neutral">Scaduto</span>'}
        </div>
        <div class="plan-card-meta">
          <span class="badge badge-neutral">${plan.sessionsPerWeek || 3}x / settimana</span>
          <span class="badge badge-neutral">${(plan.exercises || []).length} esercizi</span>
        </div>
        <ul class="plan-card-exercises">
          ${(plan.exercises || []).slice(0, 5).map(ex => `
            <li class="plan-card-exercise">
              <span class="plan-card-exercise-name">${ex.exerciseName}</span>
              <span class="plan-card-exercise-detail">${ex.targetSets}x${ex.targetReps}</span>
            </li>
          `).join('')}
          ${(plan.exercises || []).length > 5
            ? `<li class="plan-card-exercise"><span class="plan-card-exercise-detail">+${plan.exercises.length - 5} altri</span></li>`
            : ''}
        </ul>
      </div>
    `).join('');
  }
}

// ============================================================
// SESSIONS
// ============================================================
async function loadSessions() {
  const sessions = await fetchApi('/api/sessions?limit=50');
  if (!sessions || sessions.length === 0) {
    const el = document.getElementById('sessions-container');
    if (el) {
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">history</div><div class="empty-state-text">Nessuna sessione trovata</div></div>';
    }
    return;
  }

  const el = document.getElementById('sessions-container');
  if (el) {
    el.innerHTML = sessions.map(s => createSessionCard(s)).join('');
  }
}

function createSessionCard(session) {
  const setCount = session.totalSets || 0;
  return `
    <div class="session-card" onclick="window.location.href='/session/${session.id}'">
      <div class="session-card-header">
        <span class="session-card-date">${formatDate(session.timestamp)}</span>
        <span class="badge badge-primary">${session.planName || 'Sessione'}</span>
      </div>
      <div class="session-card-stats">
        <span class="session-card-stat">
          <span class="material-symbols-outlined">replay</span>
          ${setCount} set
        </span>
        ${session.noteSessione ? `
          <span class="session-card-stat">
            <span class="material-symbols-outlined">note</span>
            ${session.noteSessione}
          </span>
        ` : ''}
      </div>
    </div>
  `;
}

// ============================================================
// SESSION DETAIL
// ============================================================
async function loadSessionDetail() {
  const pathParts = window.location.pathname.split('/');
  const sessionId = pathParts[pathParts.length - 1];
  if (!sessionId) return;

  const session = await fetchApi(`/api/sessions/${sessionId}`);
  if (!session) {
    const el = document.getElementById('session-detail');
    if (el) {
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">error</div><div class="empty-state-text">Sessione non trovata</div></div>';
    }
    return;
  }

  const dateEl = document.getElementById('session-date');
  const planEl = document.getElementById('session-plan');
  const volEl = document.getElementById('session-volume');
  const setsEl = document.getElementById('session-sets');

  if (dateEl) dateEl.textContent = formatDateTime(session.timestamp);
  if (planEl) planEl.textContent = session.planName || 'Sessione';

  const volume = session.totalVolume || 0;
  const setCount = (session.sets || []).length;
  if (volEl) volEl.textContent = formatVolume(volume) + ' kg';
  if (setsEl) setsEl.textContent = setCount + ' set';

  if (!session.sets || session.sets.length === 0) {
    const el = document.getElementById('exercises-container');
    if (el) {
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">fitness_center</div><div class="empty-state-text">Nessun set registrato</div></div>';
    }
    return;
  }

  const grouped = {};
  session.sets.forEach(set => {
    if (!grouped[set.exerciseName]) grouped[set.exerciseName] = [];
    grouped[set.exerciseName].push(set);
  });

  const el = document.getElementById('exercises-container');
  if (el) {
    el.innerHTML = Object.entries(grouped).map(([name, sets]) => `
      <div class="exercise-block">
        <div class="exercise-block-header">
          <div class="exercise-block-name">${name}</div>
          <span class="badge badge-neutral">${sets.length} set</span>
        </div>
        <div class="sets-list">
          <div class="set-row set-row-header">
            <div>Serie</div>
            <div>Peso</div>
            <div>Reps</div>
            <div>RPE</div>
          </div>
          ${sets.map(set => `
            <div class="set-row">
              <div class="set-number">${set.numeroSerie}</div>
              <div>${set.pesoSollevato} kg</div>
              <div>${set.repsEffettive}</div>
              <div>${set.rpe || '-'}</div>
            </div>
          `).join('')}
        </div>
      </div>
    `).join('');
  }
}

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initNav();

  const path = window.location.pathname;
  if (path === '/') loadDashboard();
  else if (path === '/analytics') loadAnalytics();
  else if (path === '/plans') loadPlans();
  else if (path === '/sessions') loadSessions();
  else if (path.startsWith('/session/')) loadSessionDetail();
});
