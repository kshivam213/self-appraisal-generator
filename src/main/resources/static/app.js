const $ = (id) => document.getElementById(id);

const trackSelect = $("track");
const metricsBox = $("metrics");
const form = $("appraisal-form");
const submitBtn = $("submit-btn");
const spinner = submitBtn.querySelector(".spinner");
const btnLabel = submitBtn.querySelector(".btn-label");
const errorEl = $("error");
const resultCard = $("result-card");
const projectsInput = $("projects");
const charCount = $("char-count");
const metricCount = $("metric-count");

function updateProgress() {
  const identityDone = $("name").value.trim() && $("title").value.trim();
  const lensDone = Boolean(trackSelect.value);
  const evidenceDone = Boolean(projectsInput.value.trim());
  const states = { identity: identityDone, lens: lensDone, evidence: evidenceDone, draft: !resultCard.hidden };
  const completed = Object.values(states).filter(Boolean).length;
  $("completion").textContent = `${Math.round((completed / 4) * 100)}% complete`;

  let foundCurrent = false;
  document.querySelectorAll(".progress-rail li").forEach((item) => {
    const done = states[item.dataset.step];
    item.classList.toggle("done", done);
    item.classList.remove("active");
    if (!done && !foundCurrent) {
      item.classList.add("active");
      foundCurrent = true;
    }
  });
}

function updateMetricCount() {
  const count = document.querySelectorAll('input[name="metric"]:checked').length;
  metricCount.textContent = count ? `${count} selected` : "All metrics";
}

form.addEventListener("input", () => {
  charCount.textContent = `${projectsInput.value.length.toLocaleString()} / 6,000`;
  updateProgress();
});

// Load all tracks on page load and populate the dropdown.
async function loadTracks() {
  try {
    const res = await fetch("/metrics");
    if (!res.ok) throw new Error("Failed to load tracks");
    const tracks = await res.json();
    const keys = Object.keys(tracks);
    trackSelect.innerHTML = '<option value="" disabled selected>Select a track…</option>';
    keys.forEach((key) => {
      const opt = document.createElement("option");
      opt.value = key;
      opt.textContent = key.charAt(0).toUpperCase() + key.slice(1);
      trackSelect.appendChild(opt);
    });
    if (keys.length === 1) {
      trackSelect.value = keys[0];
      loadMetrics(keys[0]);
    }
  } catch (e) {
    trackSelect.innerHTML = '<option value="" disabled selected>Could not load tracks</option>';
  }
}

// Load metrics for the selected track and render checkboxes.
async function loadMetrics(track) {
  metricsBox.innerHTML = '<p class="muted">Loading metrics…</p>';
  try {
    const res = await fetch(`/metrics/${encodeURIComponent(track)}`);
    if (!res.ok) throw new Error("Failed to load metrics");
    const metrics = await res.json();
    if (!metrics.length) {
      metricsBox.innerHTML = '<p class="muted">No metrics configured for this track.</p>';
      return;
    }
    metricsBox.innerHTML = "";
    metrics.forEach((m) => {
      const label = document.createElement("label");
      label.className = "metric-item";
      label.innerHTML = `
        <input type="checkbox" value="${m.key}" name="metric" />
        <span>
          <span class="m-name">${escapeHtml(m.name)}</span><br />
          <span class="m-desc">${escapeHtml(m.description || "")}</span>
        </span>`;
      metricsBox.appendChild(label);
    });
    metricsBox.addEventListener("change", updateMetricCount);
    updateMetricCount();
  } catch (e) {
    metricsBox.innerHTML = '<p class="muted">Could not load metrics.</p>';
  }
}

trackSelect.addEventListener("change", (e) => loadMetrics(e.target.value));

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideError();

  const payload = {
    name: $("name").value.trim(),
    title: $("title").value.trim(),
    track: trackSelect.value,
    metricKeys: [...document.querySelectorAll('input[name="metric"]:checked')].map((c) => c.value),
    projects: $("projects").value.trim(),
  };

  if (!payload.name || !payload.title || !payload.track || !payload.projects) {
    showError("Please fill in name, title, track, and projects.");
    return;
  }

  setLoading(true);
  try {
    const res = await fetch("/recommend", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || "Request failed");
    }
    renderResult(data);
  } catch (err) {
    showError(err.message || "Something went wrong. Please try again.");
  } finally {
    setLoading(false);
  }
});

form.addEventListener("reset", () => {
  resultCard.hidden = true;
  hideError();
  setTimeout(() => {
    metricsBox.innerHTML = '<p class="muted">Select a track to see its review areas.</p>';
    charCount.textContent = "0 / 6,000";
    updateMetricCount();
    updateProgress();
  });
});

function renderResult(data) {
  $("result-name").textContent = data.name;
  $("result-title").textContent = data.title;
  $("result-track").textContent = data.track;

  renderOverall(data);

  const container = $("recommendation");
  container.innerHTML = "";
  const scores = data.metricScores || [];
  if (!scores.length) {
    container.textContent = data.recommendation || "";
  } else {
    scores.forEach((s, i) => container.appendChild(buildSection(s, i)));
  }

  resultCard.hidden = false;
  updateProgress();
  resultCard.scrollIntoView({ behavior: "smooth", block: "start" });
}

// Renders the overall score gauge, rating, and summary.
function renderOverall(data) {
  const box = $("overall");
  const score = typeof data.overallScore === "number" ? data.overallScore : 0;
  const rating = data.overallRating || "—";
  box.innerHTML = `
    <div class="overall-score ${scoreClass(score)}">
      <span class="os-value">${score.toFixed(1)}</span>
      <span class="os-max">/ 5</span>
    </div>
    <div class="overall-meta">
      <span class="overall-rating ${scoreClass(score)}">${escapeHtml(rating)}</span>
      <p class="overall-summary">${escapeHtml(data.summary || "")}</p>
    </div>`;
}

// Colour band based on the 1-5 score.
function scoreClass(score) {
  if (score >= 4.5) return "s-outstanding";
  if (score >= 3.5) return "s-exceeds";
  if (score >= 2.5) return "s-meets";
  return "s-low";
}

function buildSection(sec, index) {
  const wrap = document.createElement("div");
  wrap.className = "rec-section" + (index === 0 ? " open" : "");
  const score = typeof sec.score === "number" ? sec.score : 0;
  wrap.innerHTML = `
    <button type="button" class="rec-head">
      <span class="rec-title">${escapeHtml(sec.name)}</span>
      <span class="rec-head-right">
        <span class="score-badge ${scoreClass(score)}">${score.toFixed(1)}</span>
        <span class="rec-toggle">▾</span>
      </span>
    </button>
    <div class="rec-body">
      <textarea class="rec-text" rows="4"></textarea>
      <div class="rec-actions">
        <button type="button" class="btn-ghost small rec-copy">Copy section</button>
      </div>
    </div>`;
  const ta = wrap.querySelector(".rec-text");
  ta.value = sec.narrative || "";
  ta.addEventListener("input", () => autoGrow(ta));
  if (wrap.classList.contains("open")) requestAnimationFrame(() => autoGrow(ta));
  wrap.querySelector(".rec-head").addEventListener("click", () => {
    wrap.classList.toggle("open");
    if (wrap.classList.contains("open")) requestAnimationFrame(() => autoGrow(ta));
  });
  wrap.querySelector(".rec-copy").addEventListener("click", (e) => {
    copyText(`${sec.name} (${score.toFixed(1)}/5)\n${ta.value}`, e.target, "Copy section");
  });
  return wrap;
}

function autoGrow(el) {
  el.style.height = "auto";
  el.style.height = Math.max(el.scrollHeight, 96) + "px";
}

// Collect the (possibly edited) full recommendation, including scores.
function collectAll() {
  const header = [
    `Self-Appraisal for ${$("result-name").textContent}`,
    `${$("result-title").textContent} — ${$("result-track").textContent}`,
    "",
    `Overall: ${(document.querySelector(".os-value")?.textContent || "")}/5  (${document.querySelector(".overall-rating")?.textContent || ""})`,
    `${document.querySelector(".overall-summary")?.textContent || ""}`,
    "",
  ].join("\n");

  const secs = [...document.querySelectorAll(".rec-section")];
  if (!secs.length) return header + ($("recommendation").textContent || "");
  const body = secs.map((s) => {
    const heading = s.querySelector(".rec-title").textContent;
    const score = s.querySelector(".score-badge")?.textContent || "";
    const text = s.querySelector(".rec-text").value;
    return `${heading} (${score}/5)\n${text}`;
  }).join("\n\n");
  return header + body;
}

async function copyText(text, btn, resetLabel) {
  try {
    await navigator.clipboard.writeText(text);
    const original = resetLabel || btn.textContent;
    btn.textContent = "Copied!";
    setTimeout(() => (btn.textContent = original), 1500);
  } catch (_) { /* clipboard may be blocked; ignore */ }
}

$("copy-btn").addEventListener("click", (e) => copyText(collectAll(), e.target, "Copy all"));

$("download-btn").addEventListener("click", () => {
  const blob = new Blob([collectAll()], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  const who = ($("result-name").textContent || "self-appraisal").replace(/\s+/g, "-").toLowerCase();
  a.href = url;
  a.download = `${who}-appraisal.txt`;
  a.click();
  URL.revokeObjectURL(url);
});

function setLoading(loading) {
  submitBtn.disabled = loading;
  spinner.hidden = !loading;
  btnLabel.textContent = loading ? "Shaping your story…" : "Shape my appraisal";
  submitBtn.querySelector(".arrow").hidden = loading;
}

function showError(msg) { errorEl.textContent = msg; errorEl.hidden = false; }
function hideError() { errorEl.hidden = true; }

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

loadTracks();
updateProgress();
