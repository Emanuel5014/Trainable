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
    themeToggle.addEventListener('click', async () => {
      const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      html.setAttribute('data-theme', next);
      localStorage.setItem('theme', next);
      await applyThemeColors();
      refreshCharts();
    });
  }
}

function refreshCharts() {
  if (window.location.pathname !== '/analytics') return;
  const getPeriod = target => {
    const btn = document.querySelector(`.period-selector[data-target="${target}"] .period-btn.active`);
    return btn ? parseInt(btn.dataset.period) : 30;
  };
  loadBodyWeightChart(getPeriod('body-weight'));
  loadCategoryVolumeChart(getPeriod('volume-category'));
  loadPlanTonnageCharts(getPeriod('plan-tonnage'));
  loadExerciseProgress(getPeriod('exercise-progress'));
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

  root.style.setProperty('--membership-text', isDark ? '#1A1A1A' : '#FFFFFF');

  // Nav blur uses the surface color
  root.style.setProperty('--nav-blur', `rgba(${hexToRgb(colors.surface || '#0E0E11')}, ${isDark ? '0.75' : '0.8'})`);

  // Force membership card gradient repaint (color-mix in CSS may not update)
  const card = document.getElementById('membership-card');
  if (card) {
    card.style.background = `linear-gradient(135deg, ${colors.primary}, ${mixColors(colors.primary, colors.tertiary, 0.4)})`;
  }
}

function mixColors(c1, c2, p) {
  const r1 = parseInt(c1.slice(1,3), 16), g1 = parseInt(c1.slice(3,5), 16), b1 = parseInt(c1.slice(5,7), 16);
  const r2 = parseInt(c2.slice(1,3), 16), g2 = parseInt(c2.slice(3,5), 16), b2 = parseInt(c2.slice(5,7), 16);
  const r = Math.round(r1 + (r2 - r1) * p);
  const g = Math.round(g1 + (g2 - g1) * p);
  const b = Math.round(b1 + (b2 - b1) * p);
  return `#${r.toString(16).padStart(2,'0')}${g.toString(16).padStart(2,'0')}${b.toString(16).padStart(2,'0')}`;
}

function hexToRgb(hex) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `${r}, ${g}, ${b}`;
}

function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
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

function formatInteger(value) {
  if (!value && value !== 0) return '0';
  return Math.round(value).toLocaleString('it-IT');
}

function getPeriodLabel(days, startDate, endDate) {
  const s = new Date(startDate);
  const e = new Date(endDate);
  const opts = { day: 'numeric', month: 'short', year: 'numeric' };
  if (days <= 0) {
    return `${s.toLocaleDateString('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })} - ${e.toLocaleDateString('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })}`;
  }
  return `${s.toLocaleDateString('it-IT', { day: 'numeric', month: 'short' })} - ${e.toLocaleDateString('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })}`;
}

function getStartEndFromDays(days) {
  const end = new Date();
  let start;
  if (days <= 0) {
    start = new Date(2000, 0, 1);
  } else {
    start = new Date();
    start.setDate(start.getDate() - days);
  }
  return { start: start.getTime(), end: end.getTime() };
}

function updatePeriodSubtitle(headerEl, data, timestampKey, days) {
  if (!headerEl) return;
  const subtitle = headerEl.querySelector('.section-subtitle');
  if (!subtitle || !data || data.length === 0) return;
  const firstTs = data[0][timestampKey];
  const lastTs = data[data.length - 1][timestampKey];
  subtitle.textContent = getPeriodLabel(days, firstTs, lastTs);
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
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">fitness_center</div><div class="empty-state-text">' + t('noSessionsFound') + '</div></div>';
    }
  }
}

async function loadMembership() {
  const data = await fetchApi('/api/dashboard/membership');
  const el = document.getElementById('membership-card');
  if (!el) return;

  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

  if (!data || !data.expiryDate) {
    el.className = 'membership-card empty';
    el.innerHTML = `
      <div class="membership-card-header">
        <span class="material-symbols-outlined">credit_card</span>
        <span class="membership-card-title">${t('gymMembership')}</span>
      </div>
      <div class="membership-card-body">
        <div>
          <div class="membership-card-username">${data?.username || 'Athlete'}</div>
          <div class="membership-card-expiry">${t('tapToSet')}</div>
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

  const badgeText = isExpired ? t('expiredBadge') : (isExpiringSoon ? t('expiringSoon', {days: daysLeft}) : t('daysLeft', {days: daysLeft}));

  // Build gradient matching Compose GymMembershipCard logic
  const primary = getComputedStyle(document.documentElement).getPropertyValue('--md-primary').trim() || '#3A59D1';
  const tertiary = getComputedStyle(document.documentElement).getPropertyValue('--md-tertiary').trim() || '#7AC6D2';
  const error = getComputedStyle(document.documentElement).getPropertyValue('--md-error').trim() || '#FFB4AB';

  let gradient;
  if (isDark) {
    if (isExpired) {
      gradient = `linear-gradient(135deg, ${hexToRgba(error, 0.8)}, ${hexToRgba(error, 0.5)})`;
    } else if (isExpiringSoon) {
      gradient = `linear-gradient(135deg, ${hexToRgba(tertiary, 0.9)}, ${hexToRgba(tertiary, 0.6)})`;
    } else {
      gradient = `linear-gradient(135deg, ${hexToRgba(primary, 0.9)}, ${hexToRgba(primary, 0.6)})`;
    }
  } else {
    if (isExpired) {
      gradient = `linear-gradient(135deg, ${hexToRgba(error, 0.25)}, ${hexToRgba(error, 0.12)})`;
    } else if (isExpiringSoon) {
      gradient = `linear-gradient(135deg, ${hexToRgba(tertiary, 0.25)}, ${hexToRgba(tertiary, 0.12)})`;
    } else {
      gradient = `linear-gradient(135deg, ${hexToRgba(primary, 0.22)}, ${hexToRgba(primary, 0.1)})`;
    }
  }

  el.className = 'membership-card';
  el.style.background = gradient;
  el.innerHTML = `
    <svg class="membership-shape-gem" viewBox="0 0 380 380" preserveAspectRatio="xMidYMid meet">
      <path d="M133.415 65.7498C150.981 52.991 159.763 46.6116 169.124 43.4402C182.663 38.8533 197.337 38.8533 210.876 43.4402C220.237 46.6116 229.019 52.991 246.585 65.7498L286.083 94.4402C296.514 102.017 301.729 105.805 305.981 110.353C312.125 116.926 316.839 124.702 319.823 133.189C321.888 139.062 322.834 145.437 324.725 158.189L332.038 207.501C335.355 229.87 337.014 241.055 335.34 251.136C332.918 265.718 325.6 279.04 314.59 288.907C306.979 295.728 296.649 300.329 275.987 309.532L229.176 330.382C216.707 335.935 210.473 338.712 204.064 340.131C194.8 342.182 185.2 342.182 175.936 340.131C169.527 338.712 163.293 335.935 150.824 330.382L104.013 309.532C83.3515 300.329 73.0207 295.728 65.4097 288.907C54.4005 279.04 47.0819 265.718 44.6602 251.136C42.9859 241.055 44.6447 229.87 47.9621 207.501L55.2752 158.189C57.1662 145.437 58.1117 139.062 60.1768 133.189C63.1612 124.702 67.8747 116.926 74.0191 110.353C78.2706 105.805 83.4862 102.017 93.9172 94.4402L133.415 65.7498Z" fill="rgba(255,255,255,0.1)"/>
    </svg>
    <svg class="membership-shape-sunny" viewBox="0 0 380 380" preserveAspectRatio="xMidYMid meet">
      <path d="M276.453 68.8118C286.405 69.4881 291.381 69.8263 295.404 71.5853C301.223 74.1305 305.87 78.7766 308.415 84.5965C310.174 88.6186 310.512 93.5948 311.188 103.547L312.732 126.259C313.005 130.284 313.142 132.296 313.579 134.219C314.212 136.997 315.31 139.648 316.827 142.059C317.877 143.728 319.203 145.248 321.856 148.288L336.824 165.438C343.384 172.954 346.663 176.712 348.263 180.8C350.579 186.715 350.579 193.285 348.263 199.2C346.663 203.288 343.384 207.046 336.824 214.562L321.856 231.712C319.203 234.752 317.877 236.272 316.827 237.941C315.31 240.352 314.212 243.003 313.579 245.781C313.142 247.704 313.005 249.716 312.732 253.741L311.188 276.453C310.512 286.405 310.174 291.381 308.415 295.404C305.87 301.223 301.223 305.87 295.404 308.415C291.381 310.174 286.405 310.512 276.453 311.188L253.741 312.732C249.716 313.005 247.704 313.142 245.781 313.579C243.003 314.212 240.352 315.31 237.941 316.827C236.272 317.877 234.752 319.203 231.712 321.856L214.562 336.824C207.046 343.384 203.288 346.663 199.2 348.263C193.285 350.579 186.715 350.579 180.8 348.263C176.712 346.663 172.954 343.384 165.438 336.824L148.288 321.856C145.248 319.203 143.728 317.877 142.059 316.827C139.648 315.31 136.997 314.212 134.219 313.579C132.296 313.142 130.284 313.005 126.259 312.732L103.547 311.188C93.5947 310.512 88.6186 310.174 84.5965 308.415C78.7766 305.87 74.1305 301.223 71.5853 295.404C69.8263 291.381 69.4881 286.405 68.8118 276.453L67.2684 253.741C66.9949 249.716 66.8581 247.704 66.4206 245.781C65.7883 243.003 64.6903 240.352 63.173 237.941C62.123 236.272 60.7965 234.752 58.1437 231.712L43.1756 214.562C36.6164 207.046 33.3369 203.288 31.7366 199.2C29.4211 193.285 29.4211 186.715 31.7366 180.8C33.3369 176.712 36.6164 172.954 43.1756 165.438L58.1437 148.288C60.7965 145.248 62.123 143.728 63.173 142.059C64.6903 139.648 65.7883 136.997 66.4206 134.219C66.8581 132.296 66.9949 130.284 67.2684 126.259L68.8118 103.547C69.4881 93.5948 69.8263 88.6186 71.5853 84.5965C74.1305 78.7766 78.7766 74.1305 84.5965 71.5853C88.6186 69.8263 93.5948 69.4881 103.547 68.8118L126.259 67.2684C130.284 66.9949 132.296 66.8581 134.219 66.4206C136.997 65.7883 139.648 64.6903 142.059 63.173C143.728 62.123 145.248 60.7966 148.288 58.1437L165.438 43.1756C172.954 36.6164 176.712 33.3369 180.8 31.7366C186.715 29.4211 193.285 29.4211 199.2 31.7366C203.288 33.3369 207.046 36.6164 214.562 43.1756L231.712 58.1437C234.752 60.7966 236.272 62.123 237.941 63.173C240.352 64.6903 243.003 65.7883 245.781 66.4206C247.704 66.8581 249.716 66.9949 253.741 67.2684L276.453 68.8118Z" fill="rgba(0,0,0,0.05)"/>
    </svg>
    <div class="membership-card-inner">
      <div class="membership-card-header">
        <span class="material-symbols-outlined">credit_card</span>
        <span class="membership-card-title">${t('gymMembership')}</span>
      </div>
      <div class="membership-card-body">
        <div>
          <div class="membership-card-username">${data.username}</div>
          <div class="membership-card-expiry">${t('validUntil')} ${expiryText}</div>
        </div>
        <span class="membership-card-badge">${badgeText}</span>
      </div>
      ${isExpired ? '<span class="material-symbols-outlined" style="position:absolute;top:24px;right:24px;font-size:24px;opacity:0.7;">warning</span>' : ''}
    </div>
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
    <canvas class="weekly-goal-canvas-bg"></canvas>
    <div class="weekly-progress-content">
      <div class="weekly-goal-label">${t('weeklyGoalLabel')}</div>
      <div class="weekly-progress">
        <span class="weekly-progress-value">${total}</span>
        <span class="weekly-progress-target">/ ${data.weeklyGoal}</span>
        <span style="flex:1"></span>
        ${goalMet
          ? '<span class="weekly-goal-met"><span class="material-symbols-outlined" style="font-size:20px;">check_circle</span></span>'
          : ''
        }
      </div>
      ${!goalMet ? `<div class="weekly-remaining">${remaining === 1 ? t('workoutRemaining', {n: remaining}) : t('workoutsRemaining', {n: remaining})}</div>` : ''}
      ${goalMet ? '<div class="weekly-remaining goal-met">' + t('goalMet') + '</div>' : ''}
    </div>
  `;

  // Start wavy pattern animation
  animateWeeklyWave(el, total, data.weeklyGoal);
}

function animateWeeklyWave(card, current, goal) {
  const canvas = card.querySelector('.weekly-goal-canvas-bg');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const colors = getCanvasColors();
  const progress = goal > 0 ? Math.min(1, current / goal) : 0;

  let shift = 0;
  let animId;

  function draw(t) {
    const rect = card.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    canvas.style.width = rect.width + 'px';
    canvas.style.height = rect.height + 'px';
    ctx.scale(dpr, dpr);

    shift = (t / 3000) % 2;

    ctx.clearRect(0, 0, rect.width, rect.height);

    if (progress <= 0) {
      animId = requestAnimationFrame(draw);
      return;
    }

    const clamped = Math.min(1, progress);
    const height = rect.height;
    const halfPeriod = 21;
    const amp = 6;

    if (clamped >= 1) {
      ctx.fillStyle = colors.primary + '26';
      ctx.fillRect(0, 0, rect.width, height);
    } else {
      // Wave at the RIGHT edge of the filled area, fill goes LEFT (completed portion)
      const edgeX = rect.width * clamped;

      ctx.beginPath();
      ctx.moveTo(edgeX, 0);

      // Wavy edge — ensure continuous waves from top to bottom
      const phaseOffset = shift * halfPeriod;
      const startI = Math.ceil(-phaseOffset / halfPeriod);
      const firstBaseY = startI * halfPeriod + phaseOffset;

      // Draw straight segment from y=0 to first wave position
      if (firstBaseY > 0) {
        ctx.lineTo(edgeX, firstBaseY);
      }

      const wavesNeeded = Math.ceil((height - firstBaseY) / halfPeriod) + 3;

      for (let i = startI; i < startI + wavesNeeded; i++) {
        const baseY = i * halfPeriod + phaseOffset;
        if (baseY > height + halfPeriod) break;

        const direction = Math.abs(i) % 2 === 0 ? 1 : -1;
        const waveX = edgeX + amp * direction;
        const startY = Math.max(0, baseY);
        const endY = Math.min(height, baseY + halfPeriod);

        if (startY < height && endY > startY) {
          const midY = (startY + endY) / 2;
          ctx.quadraticCurveTo(waveX, midY, edgeX, endY);
        }
      }

      // Fill to the LEFT of the wave (completed portion)
      ctx.lineTo(0, height);
      ctx.lineTo(0, 0);
      ctx.closePath();
      ctx.fillStyle = colors.primary + '26';
      ctx.fill();
    }

    animId = requestAnimationFrame(draw);
  }

  animId = requestAnimationFrame(draw);
  card._cancelAnim = () => cancelAnimationFrame(animId);
}

// ============================================================
// CONSISTENCY CALENDAR (GitHub-style)
// ============================================================
async function loadConsistencyCalendar() {
  const data = await fetchApi('/api/analytics/consistency-calendar?weeks=14');
  const container = document.getElementById('calendar-container');
  if (!container || !data || !data.days) {
    if (container) container.innerHTML = '<div class="empty-state"><div class="empty-state-text">' + t('noData') + '</div></div>';
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

  const monthNames = t('monthShort');
  const dayShort = t('dayShort');
  const dayLabels = ['', dayShort[0], '', dayShort[1], '', dayShort[2], ''];

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

    const tooltip = isFuture ? '' : `${dateStr}: ${count} ${count === 1 ? t('workout') : t('workouts')}`;
    const classes = ['calendar-cell'];
    if (level > 0) classes.push('l' + level);
    if (isToday) classes.push('today');
    if (isFuture) classes.push('future');

    const onClick = level > 0 && !isFuture ? ` onclick="window.location.href='/sessions?date=${dateStr}'"` : '';
    cellsHtml += `<div class="${classes.join(' ')}" data-tooltip="${tooltip}"${onClick}></div>`;
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
        <span class="chart-title">${t('lastWeeks', {weeks: data.weeks})}</span>
        <div class="calendar-legend">
          <span>${t('less')}</span>
          <div class="legend-box l0"></div>
          <div class="legend-box l1"></div>
          <div class="legend-box l2"></div>
          <div class="legend-box l3"></div>
          <div class="legend-box l4"></div>
          <span>${t('more')}</span>
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
  setupPeriodSelectors();
  loadBodyWeightChart(30);
  loadCategoryVolumeChart(30);
  loadPlanTonnageCharts(30);
  loadExercisesDropdown();
}

function setupPeriodSelectors() {
  document.querySelectorAll('.period-selector').forEach(group => {
    const target = group.dataset.target;
    group.querySelectorAll('.period-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        group.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        let days = parseInt(btn.dataset.period);

        // For 7-day period, start from Monday of current week
        if (days === 7) {
          const now = new Date();
          const dayOfWeek = now.getDay();
          const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
          days = diff + 1;
        }

        if (target === 'body-weight') loadBodyWeightChart(days);
        else if (target === 'volume-category') loadCategoryVolumeChart(days);
        else if (target === 'plan-tonnage') loadPlanTonnageCharts(days);
        else if (target === 'exercise-progress') loadExerciseProgress(days);
      });
    });
  });
}

function getCanvasColors() {
  const style = getComputedStyle(document.documentElement);
  return {
    primary: style.getPropertyValue('--md-primary').trim() || '#3A59D1',
    tertiary: style.getPropertyValue('--md-tertiary').trim() || '#7AC6D2',
    onSurface: style.getPropertyValue('--md-on-surface').trim() || '#E7E1EC',
    onSurfaceVariant: style.getPropertyValue('--md-on-surface-variant').trim() || '#ADA9B3',
    surfaceContainerHigh: style.getPropertyValue('--md-surface-container-high').trim() || '#201F25',
    surfaceContainer: style.getPropertyValue('--md-surface-container').trim() || '#1A191E',
  };
}

function loadBodyWeightChart(days) {
  const headerEl = document.getElementById('body-weight-chart')?.previousElementSibling;
  fetchApi(`/api/analytics/body-weight?days=${days}`).then(data => {
    updatePeriodSubtitle(headerEl, data, 'timestamp', days);
    renderLineChart('body-weight-canvas', data, 'weight', t('weightUnit'), v => { const w = __weightUnit === 'lb' ? v * 2.20462 : v; return w >= 100 ? w.toFixed(0) : w.toFixed(1); });
  });
}

function loadCategoryVolumeChart(days) {
  const headerEl = document.getElementById('volume-category-chart')?.previousElementSibling;
  if (headerEl) {
    const subtitle = headerEl.querySelector('.section-subtitle');
    if (subtitle) subtitle.textContent = days <= 0 ? t('periodAll') : getPeriodLabel(days, Date.now() - days * 86400000, Date.now());
  }
  fetchApi(`/api/analytics/volume-by-category?days=${days}`).then(data => {
    renderBarChart('volume-category-canvas', data, 'category', 'volume');
  });
}

let _exercisesCache = [];

async function loadExercisesDropdown() {
  const autocomplete = document.getElementById('exercise-autocomplete');
  if (!autocomplete) return;
  const exercises = await fetchApi('/api/exercises');
  if (!exercises) return;
  _exercisesCache = exercises;
  autocomplete.innerHTML = exercises.map(e =>
    `<m3e-option value="${e.name}">${e.name}</m3e-option>`
  ).join('');
  autocomplete.addEventListener('change', () => {
    if (autocomplete.value) {
      loadExerciseProgress(getActivePeriod('exercise-progress'));
    } else {
      const canvas = document.getElementById('exercise-progress-canvas');
      const summary = document.getElementById('exercise-progress-summary');
      if (canvas) canvas.style.display = 'none';
      if (summary) summary.innerHTML = '';
    }
  });
}

function loadExerciseProgress(days) {
  const autocomplete = document.getElementById('exercise-autocomplete');
  const exerciseName = autocomplete ? autocomplete.value : null;
  if (!exerciseName) return;
  const exercise = _exercisesCache.find(e => e.name === exerciseName);
  if (!exercise) return;
  loadExerciseProgressChart(exercise.id, days);
}

async function loadExerciseProgressChart(exerciseId, days) {
  const headerEl = document.getElementById('exercise-progress-chart')?.previousElementSibling;
  const loadingEl = document.getElementById('exercise-progress-loading');
  const canvas = document.getElementById('exercise-progress-canvas');
  const summaryEl = document.getElementById('exercise-progress-summary');
  if (!canvas || !summaryEl) return;

  if (loadingEl) loadingEl.style.display = 'flex';
  canvas.style.display = 'none';

  const data = await fetchApi(`/api/analytics/exercise-progress/${exerciseId}?days=${days}`);

  if (loadingEl) loadingEl.style.display = 'none';

  if (!data || data.length === 0) {
    canvas.style.display = 'none';
    summaryEl.innerHTML = '<div class="empty-state-text">' + t('noDataPeriod') + '</div>';
    return;
  }

  canvas.style.display = 'block';

  const latest = data[data.length - 1].value;
  const first = data[0].value;
  const change = first > 0 ? ((latest - first) / first) * 100 : 0;

  summaryEl.innerHTML = `
    <span class="current-1rm">${formatInteger(latest)}</span>
    <span class="1rm-unit">${t('kgShort')}</span>
    ${change !== 0 ? `<span class="1rm-change ${change > 0 ? 'positive' : 'negative'}">${change > 0 ? '+' : ''}${change.toFixed(1)}%</span>` : ''}
  `;

  renderLineChart('exercise-progress-canvas', data, 'value', t('weightUnit'), v => { const w = __weightUnit === 'lb' ? v * 2.20462 : v; return w >= 100 ? w.toFixed(0) : w.toFixed(1); });
  if (headerEl) {
    const subtitle = headerEl.querySelector('.section-subtitle');
    if (subtitle) {
      const firstTs = data[0].timestamp;
      const lastTs = data[data.length - 1].timestamp;
      subtitle.textContent = days <= 0 ? t('periodAll') : getPeriodLabel(days, firstTs, lastTs);
    }
  }
}

function getActivePeriod(target) {
  const btn = document.querySelector(`.period-selector[data-target="${target}"] .period-btn.active`);
  return btn ? parseInt(btn.dataset.period) : 30;
}

async function loadPlanTonnageCharts(days) {
  const headerEl = document.getElementById('plan-tonnage-charts')?.previousElementSibling;

  const plans = await fetchApi('/api/plans');
  const container = document.getElementById('plan-tonnage-charts');
  if (!container) return;

  const activePlans = (plans || []).filter(p => p.isActive);

  if (activePlans.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-text">' + t('noActivePlans') + '</div></div>';
    return;
  }

  container.innerHTML = '';
  let allTimestamps = [];

  for (const plan of activePlans) {
    const chartDiv = document.createElement('div');
    chartDiv.className = 'tonnage-chart';
    chartDiv.innerHTML = `
      <div class="tonnage-chart-title">${plan.name}</div>
      <div class="tonnage-chart-badge">${t('active')}</div>
      <div class="tonnage-chart-canvas-wrap"><canvas id="tonnage-canvas-${plan.id}" style="height:200px;"></canvas></div>
    `;
    container.appendChild(chartDiv);

    const planData = await fetchApi(`/api/analytics/plan-volume-history/${plan.id}?days=${days}`);
    if (planData && planData.length > 0) {
      planData.forEach(d => allTimestamps.push(d.timestamp));
      renderLineChart(`tonnage-canvas-${plan.id}`, planData, 'volume', t('weightUnit'), v => { const w = __weightUnit === 'lb' ? v * 2.20462 : v; return Math.round(w).toLocaleString('it-IT'); });
    } else {
      const canvas = document.getElementById(`tonnage-canvas-${plan.id}`);
      if (canvas) {
        const wrap = canvas.parentElement;
        const colors = getCanvasColors();
        const dpr = window.devicePixelRatio || 1;
        const wrapRect = wrap.getBoundingClientRect();
        canvas.width = wrapRect.width * dpr;
        canvas.height = 200 * dpr;
        canvas.style.width = wrapRect.width + 'px';
        canvas.style.height = '200px';
        const ctx = canvas.getContext('2d');
        ctx.scale(dpr, dpr);
        ctx.fillStyle = colors.onSurfaceVariant;
        ctx.font = '14px Google Sans Flex, Outfit, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(t('noDataPeriod'), wrapRect.width / 2, 110);
      }
    }
  }

  // Update subtitle with combined date range
  if (allTimestamps.length > 0 && headerEl) {
    const subtitle = headerEl.querySelector('.section-subtitle');
    if (subtitle) {
      const firstTs = Math.min(...allTimestamps);
      const lastTs = Math.max(...allTimestamps);
      subtitle.textContent = getPeriodLabel(days, firstTs, lastTs);
    }
  }
}

// ============================================================
// LINE CHART (with HTML tooltip, smooth bezier curves)
// ============================================================
function renderLineChart(canvasId, data, valueKey, unit, formatFn) {
  const canvas = document.getElementById(canvasId);
  if (!canvas || !data || data.length === 0) {
    if (canvas) {
      const parent = canvas.parentElement;
      parent.innerHTML = '<div class="empty-state" style="padding:30px"><div class="empty-state-text">' + t('noData') + '</div></div>';
    }
    return;
  }

  requestAnimationFrame(() => {
    drawLineChart(canvas, data, valueKey, unit, formatFn);
  });
}

function getContainerContentWidth(container) {
  const style = getComputedStyle(container);
  const rect = container.getBoundingClientRect();
  return rect.width - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight) - parseFloat(style.borderLeftWidth) - parseFloat(style.borderRightWidth);
}

function drawLineChart(canvas, data, valueKey, unit, formatFn) {
  const colors = getCanvasColors();
  const ctx = canvas.getContext('2d');
  const container = canvas.parentElement;
  const dpr = window.devicePixelRatio || 1;

  const fmt = formatFn || formatVolume;
  const canvasH = parseInt(canvas.style.height) || 200;
  const canvasW = getContainerContentWidth(container);

  const padding = { top: 24, bottom: 32, left: 56, right: 56 };
  const drawW = Math.max(10, canvasW - padding.left - padding.right);
  const drawH = Math.max(10, canvasH - padding.top - padding.bottom);

  canvas.width = canvasW * dpr;
  canvas.height = canvasH * dpr;
  canvas.style.width = canvasW + 'px';
  canvas.style.height = canvasH + 'px';
  ctx.scale(dpr, dpr);

  const values = data.map(d => d[valueKey]);
  const maxVal = Math.max(...values);
  const minVal = Math.min(...values);
  const range = maxVal - minVal || 1;

  ctx.clearRect(0, 0, canvasW, canvasH);

  // Grid lines
  ctx.strokeStyle = colors.surfaceContainerHigh;
  ctx.lineWidth = 1;
  ctx.beginPath();
  for (let i = 0; i <= 4; i++) {
    const y = padding.top + (drawH / 4) * i;
    ctx.moveTo(padding.left, y);
    ctx.lineTo(padding.left + drawW, y);
  }
  ctx.stroke();

  // Y-axis labels
  ctx.font = '10px Google Sans Flex, Outfit, sans-serif';
  ctx.fillStyle = colors.onSurfaceVariant;
  ctx.textAlign = 'right';
  ctx.textBaseline = 'middle';
  for (let i = 0; i <= 4; i++) {
    const y = padding.top + (drawH / 4) * i;
    const val = maxVal - (range / 4) * i;
    ctx.fillText(fmt(val) + ' ' + unit, padding.left - 8, y);
  }

  // Compute points
  const points = data.map((d, i) => ({
    x: padding.left + (data.length > 1 ? (i / (data.length - 1)) * drawW : drawW / 2),
    y: padding.top + drawH - ((d[valueKey] - minVal) / range) * drawH,
    value: d[valueKey],
    date: new Date(d.timestamp),
  }));

  // Gradient fill with bezier
  ctx.beginPath();
  ctx.moveTo(points[0].x, padding.top + drawH);
  ctx.lineTo(points[0].x, points[0].y);
  drawSmoothCurve(ctx, points);
  ctx.lineTo(points[points.length - 1].x, padding.top + drawH);
  ctx.closePath();

  const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + drawH);
  gradient.addColorStop(0, hexToRgba(colors.primary, 0.15));
  gradient.addColorStop(1, hexToRgba(colors.primary, 0.0));
  ctx.fillStyle = gradient;
  ctx.fill();

  // Line
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  drawSmoothCurve(ctx, points);
  ctx.strokeStyle = colors.primary;
  ctx.lineWidth = 2.5;
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';
  ctx.stroke();

  // Dots
  points.forEach(p => {
    ctx.beginPath();
    ctx.arc(p.x, p.y, 3.5, 0, Math.PI * 2);
    ctx.fillStyle = colors.primary;
    ctx.fill();
    ctx.strokeStyle = hexToRgba(colors.primary, 0.3);
    ctx.lineWidth = 1;
    ctx.stroke();
  });

  // X-axis labels
  ctx.font = '10px Google Sans Flex, Outfit, sans-serif';
  ctx.fillStyle = colors.onSurfaceVariant;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'top';

  const maxLabels = Math.max(2, Math.floor(drawW / 70));
  const labelStep = Math.max(1, Math.floor(data.length / maxLabels));
  for (let i = 0; i < data.length; i += labelStep) {
    const d = new Date(data[i].timestamp);
    const label = d.toLocaleDateString('it-IT', { day: 'numeric', month: 'short' });
    ctx.fillText(label, points[i].x, canvasH - padding.bottom + 6);
  }

  // === HTML Tooltip (no canvas redraw needed) ===
  let tooltipEl = container.querySelector('.chart-tooltip');
  if (!tooltipEl) {
    tooltipEl = document.createElement('div');
    tooltipEl.className = 'chart-tooltip';
    container.appendChild(tooltipEl);
  }

  canvas.onmousemove = function(e) {
    const rect2 = canvas.getBoundingClientRect();
    const mx = (e.clientX - rect2.left) * dpr;
    const my = (e.clientY - rect2.top) * dpr;

    let minDist = 40 * dpr;
    let closest = -1;
    points.forEach((p, i) => {
      const dist = Math.sqrt((p.x * dpr - mx) ** 2 + (p.y * dpr - my) ** 2);
      if (dist < minDist) {
        minDist = dist;
        closest = i;
      }
    });

    if (closest >= 0) {
      const p = points[closest];
      tooltipEl.style.display = 'block';
      tooltipEl.innerHTML = `<strong>${fmt(p.value)} ${unit}</strong><br><span>${p.date.toLocaleDateString('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })}</span>`;

      // Position tooltip - flip when near edges
      const tooltipW = 160;
      const tooltipH = 50;
      let tx = p.x + 12;
      let ty = p.y - tooltipH - 8;

      if (tx + tooltipW > canvasW - 10) tx = p.x - tooltipW - 12;
      if (tx < 10) tx = 10;
      if (ty < 10) ty = p.y + 16;
      if (ty + tooltipH > canvasH - 10) ty = canvasH - tooltipH - 10;

      tooltipEl.style.left = tx + 'px';
      tooltipEl.style.top = ty + 'px';
      canvas.style.cursor = 'pointer';
    } else {
      tooltipEl.style.display = 'none';
      canvas.style.cursor = 'default';
    }
  };

  canvas.addEventListener('mouseleave', () => {
    tooltipEl.style.display = 'none';
    canvas.style.cursor = 'default';
  });
}

function drawSmoothCurve(ctx, points) {
  if (points.length < 2) return;
  if (points.length === 2) {
    ctx.lineTo(points[1].x, points[1].y);
    return;
  }

  for (let i = 1; i < points.length - 1; i++) {
    const p0 = points[i - 1];
    const p1 = points[i];
    const p2 = points[i + 1];

    const cp1x = p0.x + (p1.x - p0.x) * 0.5;
    const cp1y = p0.y + (p1.y - p0.y) * 0.5;
    const cp2x = p1.x - (p2.x - p0.x) * 0.15;
    const cp2y = p1.y - (p2.y - p0.y) * 0.15;

    ctx.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, p1.x, p1.y);
  }

  const last = points[points.length - 1];
  ctx.lineTo(last.x, last.y);
}

// ============================================================
// BAR CHART
// ============================================================
function renderBarChart(canvasId, data, labelKey, valueKey) {
  const canvas = document.getElementById(canvasId);
  if (!canvas || !data || data.length === 0) {
    if (canvas) {
      const parent = canvas.parentElement;
      parent.innerHTML = '<div class="empty-state" style="padding:30px"><div class="empty-state-text">' + t('noDataPeriod') + '</div></div>';
    }
    return;
  }

  requestAnimationFrame(() => {
    drawBarChart(canvas, data, labelKey, valueKey);
  });
}

function drawBarChart(canvas, data, labelKey, valueKey) {
  const colors = getCanvasColors();
  const ctx = canvas.getContext('2d');
  const container = canvas.parentElement;
  const dpr = window.devicePixelRatio || 1;

  const barH = 28;
  const gap = 8;
  const padding = { top: 8, bottom: 8, left: 110, right: 70 };
  const chartH = padding.top + padding.bottom + data.length * (barH + gap) - gap;
  const canvasW = getContainerContentWidth(container);
  const drawW = Math.max(10, canvasW - padding.left - padding.right);

  canvas.width = canvasW * dpr;
  canvas.height = chartH * dpr;
  canvas.style.width = canvasW + 'px';
  canvas.style.height = chartH + 'px';
  ctx.scale(dpr, dpr);

  const maxVal = Math.max(...data.map(d => d[valueKey]));

  ctx.clearRect(0, 0, canvasW, chartH);
  ctx.font = '13px Google Sans Flex, Outfit, sans-serif';

  data.forEach((d, i) => {
    const y = padding.top + i * (barH + gap);
    const barW = maxVal > 0 ? (drawW * d[valueKey]) / maxVal : 0;

    ctx.fillStyle = colors.onSurfaceVariant;
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';
    const label = d[labelKey].length > 20 ? d[labelKey].substring(0, 18) + '\u2026' : d[labelKey];
    ctx.fillText(label, padding.left - 10, y + barH / 2);

    const gradient = ctx.createLinearGradient(padding.left, 0, padding.left + barW, 0);
    gradient.addColorStop(0, colors.primary);
    gradient.addColorStop(1, colors.tertiary);
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.roundRect(padding.left, y, barW, barH, 6);
    ctx.fill();

    ctx.fillStyle = colors.onSurface;
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.font = '12px Google Sans Flex, Outfit, sans-serif';
    ctx.fillText(formatVolume(d[valueKey]), padding.left + barW + 8, y + barH / 2);
  });
}

function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

// ============================================================
// PLANS
// ============================================================
async function loadPlans() {
  const plans = await fetchApi('/api/plans');

  // Setup tabs with animated slider
  const tabsContainer = document.querySelector('.plans-tabs');
  const bg = tabsContainer?.querySelector('.plans-tabs-bg');
  if (bg) bg.classList.add('left');
  const tabs = document.querySelectorAll('.plan-tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      if (bg) {
        bg.classList.remove('left', 'right');
        bg.classList.add(tab.dataset.filter === 'archived' ? 'right' : 'left');
      }
      renderPlans(plans, tab.dataset.filter);
    });
  });

  renderPlans(plans, 'active');
}

function renderPlans(plans, filter) {
  const el = document.getElementById('plans-container');
  if (!el) return;

  if (!plans || plans.length === 0) {
    el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">playlist_add_check</div><div class="empty-state-text">' + t('noPlansFound') + '</div></div>';
    return;
  }

  const userPlans = plans.filter(p => p.note !== 'SYSTEM_PLAN');
  const filtered = filter === 'active'
    ? userPlans.filter(p => p.isActive)
    : userPlans.filter(p => !p.isActive);

  if (filtered.length === 0) {
    el.innerHTML = `<div class="empty-state"><div class="empty-state-icon">playlist_add_check</div><div class="empty-state-text">${filter === 'active' ? t('noActivePlans') : t('noArchivedPlans')}</div></div>`;
    return;
  }

  // Store all plans data globally for modal access
  window._planData = filtered;

  el.innerHTML = filtered.map(plan => {
    const startLabel = formatDate(plan.startDate);
    const endLabel = plan.endDate ? formatDate(plan.endDate) : null;
    const imageCount = (plan.images || []).length;
    const allExercises = plan.exercises || [];

    return `
    <div class="plan-card" onclick="openPlanModal('${plan.id}')">
      <div class="plan-card-header">
        <div class="plan-card-name">${plan.name}</div>
        ${plan.isActive
          ? '<span class="badge badge-success">' + t('active') + '</span>'
          : '<span class="badge badge-neutral">' + t('archived') + '</span>'}
      </div>
      <div class="plan-card-dates">
        <span class="date-badge date-start">
          <span class="material-symbols-outlined">calendar_today</span>
          ${startLabel}
        </span>
        ${endLabel ? `
        <span class="date-badge ${plan.endDate && plan.endDate < Date.now() ? 'date-expired' : 'date-end'}">
          <span class="material-symbols-outlined">event</span>
          ${endLabel}
        </span>` : ''}
        <span class="badge badge-neutral">${t('exercisesCount', {n: allExercises.length})}${imageCount > 0 ? ', ' + t('photosCount', {n: imageCount}) : ''}</span>
      </div>
      <div class="plan-card-muscle-groups">
        ${[...new Set(allExercises.map(e => e.category))].filter(Boolean).slice(0, 4).map(cat => `
          <span class="badge badge-neutral">${cat}</span>
        `).join('')}
      </div>
    </div>`;
  }).join('');
}

function openPlanModal(planId) {
  const plan = window._planData?.find(p => String(p.id) === planId);
  if (!plan) return;

  const overlay = document.getElementById('plan-modal-overlay');
  const content = document.getElementById('plan-modal-content');
  if (!overlay || !content) return;

  const startLabel = formatDate(plan.startDate);
  const endLabel = plan.endDate ? formatDate(plan.endDate) : null;
  const allExercises = plan.exercises || [];
  const images = plan.images || [];

  content.innerHTML = `
    <div class="plan-modal-title">${plan.name}</div>
    <div class="plan-modal-badge ${plan.isActive ? 'active' : 'inactive'}">${plan.isActive ? t('active') : t('archived')}</div>
    <div class="plan-modal-dates">
      <span class="date-badge date-start">
        <span class="material-symbols-outlined">calendar_today</span>
        ${t('startDate')} ${startLabel}
      </span>
      ${endLabel ? `
      <span class="date-badge ${plan.endDate && plan.endDate < Date.now() ? 'date-expired' : 'date-end'}">
        <span class="material-symbols-outlined">event</span>
        ${t('endDate')} ${endLabel}
      </span>` : ''}
      <span class="badge badge-neutral">${t('exercisesCount', {n: allExercises.length})}</span>
      ${images.length > 0 ? '<span class="badge badge-neutral">' + t('photosCount', {n: images.length}) + '</span>' : ''}
    </div>

    <div class="plan-modal-section-title">${t('exercise')}</div>
    ${allExercises.length === 0 ? '<div style="color:var(--md-on-surface-variant);font-size:0.85rem;">' + t('noExercisesPlan') + '</div>' : ''}
    ${allExercises.map(ex => `
      <div class="plan-modal-exercise">
        <span class="plan-modal-exercise-name">${ex.exerciseName}</span>
        <span class="plan-modal-exercise-detail">${ex.targetSets}x${ex.targetReps}</span>
      </div>
    `).join('')}

    ${images.length > 0 ? `
    <div class="plan-modal-section-title">${t('photos')}</div>
    <div class="plan-modal-images">
      ${images.map(uri => {
        const safeUri = encodeURIComponent(uri);
        return `<img src="/api/plan-image?uri=${safeUri}" class="plan-modal-image" onclick="event.stopPropagation();openImageViewer(this.src)">`;
      }).join('')}
    </div>` : ''}
  `;

  overlay.classList.add('open');
}

function closePlanModal() {
  const overlay = document.getElementById('plan-modal-overlay');
  if (overlay) overlay.classList.remove('open');
}

// Close modal on Escape key
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closePlanModal();
    closeImageViewer();
  }
});

// Image viewer
function openImageViewer(src) {
  const overlay = document.getElementById('image-viewer-overlay');
  const img = document.getElementById('image-viewer-img');
  if (!overlay || !img) return;
  img.src = src;
  overlay.classList.add('open');
}

function closeImageViewer() {
  const overlay = document.getElementById('image-viewer-overlay');
  if (overlay) overlay.classList.remove('open');
}

// ============================================================
// SESSIONS
// ============================================================
async function loadSessions() {
  const params = new URLSearchParams(window.location.search);
  const dateParam = params.get('date');
  const planParam = params.get('planId');
  const daysParam = params.get('days');

  // Load plans for filter dropdown
  const plans = await fetchApi('/api/plans');
  const userPlans = (plans || []).filter(p => p.note !== 'SYSTEM_PLAN');
  const planSelect = document.getElementById('filter-plan');
  if (planSelect) {
    planSelect.innerHTML = '<option value="">' + t('allPlans') + '</option>' +
      userPlans.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
    if (planParam) planSelect.value = planParam;
  }

  // Set active period in modal from URL
  const periodGroup = document.querySelector('.period-selector[data-target="sessions"]');
  if (periodGroup && daysParam) {
    periodGroup.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    const match = periodGroup.querySelector(`.period-btn[data-period="${daysParam}"]`);
    if (match) match.classList.add('active');
  }

  // Update filter info badges
  updateFilterBadges(planParam, daysParam, dateParam);

  // Build API URL
  let apiUrl = '/api/sessions?limit=50';
  if (dateParam) {
    apiUrl += `&date=${dateParam}`;
  } else {
    const planId = planParam || '';
    const days = daysParam || '30';
    if (planId) apiUrl += `&planId=${planId}`;
    if (days && days !== '0') apiUrl += `&days=${days}`;
  }

  const sessions = await fetchApi(apiUrl);
  if (!sessions || sessions.length === 0) {
    const el = document.getElementById('sessions-container');
    if (el) {
      el.innerHTML = `<div class="empty-state"><div class="empty-state-icon">history</div><div class="empty-state-text">${dateParam ? t('noSessionsDate') : t('noSessionsFound')}</div></div>`;
    }
    return;
  }

  const el = document.getElementById('sessions-container');
  if (el) {
    el.innerHTML = sessions.map(s => createSessionCard(s)).join('');
  }
}

function updateFilterBadges(planId, days, date) {
  const badgePlan = document.getElementById('filter-badge-plan');
  const badgePeriod = document.getElementById('filter-badge-period');
  if (!badgePlan || !badgePeriod) return;

  if (date) {
    badgePlan.style.display = 'none';
    badgePeriod.textContent = formatDate(parseInt(date));
    badgePeriod.style.display = 'inline-flex';
    return;
  }

  const plans = window._sessionPlans || [];
  const selectedPlan = planId ? plans.find(p => p.id == planId) : null;
  if (selectedPlan) {
    badgePlan.textContent = selectedPlan.name;
    badgePlan.style.display = 'inline-flex';
  } else {
    badgePlan.style.display = 'none';
  }

  if (days && days !== '0') {
    badgePeriod.textContent = t('lastDays', {days: days});
    badgePeriod.style.display = 'inline-flex';
  } else {
    badgePeriod.textContent = t('periodAll');
    badgePeriod.style.display = 'inline-flex';
  }
}

function openFilterModal() {
  // Store current plans for the badge
  fetchApi('/api/plans').then(plans => {
    window._sessionPlans = (plans || []).filter(p => p.note !== 'SYSTEM_PLAN');
  });
  document.getElementById('filter-modal-overlay')?.classList.add('open');
}

function closeFilterModal() {
  document.getElementById('filter-modal-overlay')?.classList.remove('open');
}

function applyFilters() {
  const planSelect = document.getElementById('filter-plan');
  const periodGroup = document.querySelector('.period-selector[data-target="sessions"]');
  const activePeriod = periodGroup?.querySelector('.period-btn.active');
  const planId = planSelect ? planSelect.value : '';
  const days = activePeriod ? activePeriod.dataset.period : '30';

  const params = new URLSearchParams();
  if (planId) params.set('planId', planId);
  if (days && days !== '0') params.set('days', days);
  const qs = params.toString();
  const newUrl = '/sessions' + (qs ? '?' + qs : '');
  window.history.replaceState(null, '', newUrl);
  closeFilterModal();
  loadSessions();
}

function resetFilters() {
  const planSelect = document.getElementById('filter-plan');
  if (planSelect) planSelect.value = '';
  const periodGroup = document.querySelector('.period-selector[data-target="sessions"]');
  if (periodGroup) {
    periodGroup.querySelectorAll('.period-btn').forEach((b, i) => {
      b.classList.toggle('active', i === 1);
    });
  }
  window.history.replaceState(null, '', '/sessions');
  closeFilterModal();
  loadSessions();
}

function createSessionCard(session) {
  const volume = session.totalVolume || 0;
  const setCount = session.totalSets || 0;
  return `
    <div class="session-card" onclick="window.location.href='/session/${session.id}'">
      <div class="session-card-header">
        <span class="session-card-date">${formatDate(session.timestamp)}</span>
        <span class="session-card-plan-name">${session.planName || t('session')}</span>
      </div>
      <div class="session-card-stats">
        <span class="session-card-stat">
          <span class="material-symbols-outlined">fitness_center</span>
          ${weightText(volume)}
        </span>
        <span class="session-card-stat">
          <span class="material-symbols-outlined">replay</span>
          ${setCount} ${t('setShort')}
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
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">error</div><div class="empty-state-text">' + t('sessionNotFound') + '</div></div>';
    }
    return;
  }

  const dateEl = document.getElementById('session-date');
  const planEl = document.getElementById('session-plan');
  const volEl = document.getElementById('session-volume');
  const setsEl = document.getElementById('session-sets');
  const planBadgeEl = document.getElementById('session-plan-badge');

  if (dateEl) dateEl.textContent = formatDateTime(session.timestamp);
  if (planEl) planEl.textContent = session.planName || t('session');

  const volume = session.totalVolume || 0;
  const setCount = (session.sets || []).length;
  if (volEl) volEl.textContent = weightText(volume);
  if (setsEl) setsEl.textContent = setCount + ' ' + t('setShort');
  if (planBadgeEl) planBadgeEl.textContent = session.planName || '';

  if (!session.sets || session.sets.length === 0) {
    const el = document.getElementById('exercises-container');
    if (el) {
      el.innerHTML = '<div class="empty-state"><div class="empty-state-icon">fitness_center</div><div class="empty-state-text">' + t('noSetsRecorded') + '</div></div>';
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
          <span class="badge badge-neutral">${sets.length} ${t('setShort')}</span>
        </div>
        <div class="sets-list">
          <div class="set-row set-row-header">
            <div>${t('series')}</div>
            <div>${t('weight')}</div>
            <div>${t('reps')}</div>
          </div>
          ${sets.map(set => `
            <div class="set-row">
              <div class="set-number">${set.numeroSerie}</div>
              <div>${weightText(set.pesoSollevato)}</div>
              <div>${set.repsEffettive}</div>
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
document.addEventListener('DOMContentLoaded', async () => {
  await loadPreferences();
  translatePage();
  initTheme();
  initNav();

  // Update period buttons with translated text
  document.querySelectorAll('[data-i18n^="period"]').forEach(el => {
    el.textContent = t(el.getAttribute('data-i18n'));
  });

  const path = window.location.pathname;
  if (path === '/') loadDashboard();
  else if (path === '/analytics') loadAnalytics();
  else if (path === '/plans') loadPlans();
  else if (path === '/sessions') loadSessions();
  else if (path.startsWith('/session/')) loadSessionDetail();
});

