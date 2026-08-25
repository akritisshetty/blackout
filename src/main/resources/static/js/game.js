/* BLACKOUT // game.js - the whole game: one mission at a time, four types cycling */
(function () {
  'use strict';

  const { el, $, $all, toast, setStatus, short } = UI;

  const CYCLE = ['SEAL_INTEL', 'CRACK_BROADCAST', 'TAMPER_HUNT', 'SECRET_DROP'];
  const TITLES = {
    SEAL_INTEL: 'SEAL THE INTEL',
    CRACK_BROADCAST: 'CRACK THE CODE',
    TAMPER_HUNT: 'FIND THE FAKE',
    SECRET_DROP: 'SECRET DROP'
  };
  const POINTS = {
    SEAL_INTEL: 10,
    CRACK_BROADCAST: 15,
    TAMPER_HUNT: 20,
    SECRET_DROP: 25
  };
  const BLURB = {
    SEAL_INTEL: 'Encrypt a message with the Playfair cipher.',
    CRACK_BROADCAST: 'Decrypt an intercepted message.',
    TAMPER_HUNT: 'One of three packages was tampered with. Find it.',
    SECRET_DROP: 'Unlock a hidden keyword with your RSA badge, then decrypt.'
  };

  let profile = null;
  let mission = null;
  let assistedUsed = false;

  /* ================= session ================= */

  function agentName() { return localStorage.getItem('blackout.agent'); }

  async function boot() {
    const name = agentName();
    if (name) {
      try { profile = await API.dossier(name); } catch (e) { localStorage.removeItem('blackout.agent'); }
    }
    paintChip();
    renderPlay();
  }

  async function startGame() {
    const codename = $('#codename-input').value.trim();
    if (!codename) { toast('Type a codename first', true); return; }
    try {
      setStatus('busy', 'enlisting...');
      profile = await API.enlist(codename);
      localStorage.setItem('blackout.agent', profile.codename);
      if (Badge.supported()) {
        await Badge.ensure(profile.codename, profile.hasBadge); // silent RSA badge mint
        profile = await API.dossier(profile.codename);
      }
      paintChip();
      mission = null;
      renderPlay();
      setStatus('idle', 'welcome, agent ' + profile.codename);
    } catch (err) {
      setStatus('breach', err.message);
      toast(err.message, true);
    }
  }

  function exitGame() {
    localStorage.removeItem('blackout.agent');
    profile = null;
    mission = null;
    paintChip();
    renderPlay();
  }

  function paintChip() {
    const chip = $('#agent-chip');
    if (!profile) { chip.classList.remove('on'); return; }
    chip.classList.add('on');
    $('#chip-codename').textContent = profile.codename;
    $('#chip-score').textContent = profile.score;
    $('#chip-solved').textContent = profile.missionsSolved + ' solved';
  }

  /* ================= view switching ================= */

  $all('.tab').forEach((tab) => tab.addEventListener('click', () => {
    $all('.tab').forEach((t) => t.classList.toggle('active', t === tab));
    $all('.view').forEach((v) => v.classList.toggle('active', v.id === 'view-' + tab.dataset.view));
    if (tab.dataset.view === 'top') renderTop();
  }));

  /* ================= play view ================= */

  function renderPlay() {
    $('#name-form').style.display = profile ? 'none' : 'block';
    const area = $('#play-area');
    area.innerHTML = '';
    if (!profile) return;
    area.append(mission ? renderMissionCard() : renderLauncher());
  }

  function renderLauncher() {
    const type = CYCLE[profile.missionsSolved % CYCLE.length];
    return el('div', { class: 'panel' },
      el('h2', { class: 'panel-title', text: '// MISSION #' + (profile.missionsSolved + 1) }),
      el('div', { style: 'text-align:center;padding:24px 0' },
        el('div', { style: 'font-size:22px;letter-spacing:4px;color:var(--neon)', text: TITLES[type] }),
        el('div', { class: 'hint', style: 'margin:10px 0 20px', text: BLURB[type] }),
        el('button', {
          class: 'btn primary',
          style: 'font-size:16px;padding:12px 28px',
          onclick: startMission
        }, 'START MISSION (+' + POINTS[type] + ' pts)')));
  }

  async function startMission() {
    const type = CYCLE[profile.missionsSolved % CYCLE.length];
    try {
      setStatus('busy', 'drawing mission...');
      if (type === 'SECRET_DROP') {
        await Badge.ensure(agentName(), profile.hasBadge);
      }
      mission = await API.newMission(agentName(), type);
      assistedUsed = false;
      renderPlay();
      setStatus('secure', TITLES[type] + ' is live');
    } catch (err) {
      setStatus('breach', err.message);
      toast(err.message, true);
    }
  }

  /* ---------- shared pieces ---------- */

  function header(type) {
    return [
      el('h2', { class: 'panel-title', text: '// ' + TITLES[type] + '   (+' + POINTS[type] + ' pts)' }),
      el('p', { class: 'hint', text: BLURB[type] })
    ];
  }

  function row(labelText, content) {
    return el('div', {},
      el('label', { class: 'label', text: labelText }),
      content);
  }

  function mono(value, cls) {
    return el('div', { class: 'mono-block ' + (cls || ''), text: value });
  }

  function answerBox(placeholder) {
    return el('textarea', { placeholder, spellcheck: 'false' });
  }

  function gridBlock(keyword) {
    const host = el('div');
    API.playfairGrid(keyword)
      .then((data) => {
        host.innerHTML = '';
        const grid = el('div', { class: 'pf-grid' });
        data.matrix.forEach((r) => {
          for (const ch of r) grid.append(el('div', { class: 'pf-cell', text: ch }));
        });
        host.append(grid);
      })
      .catch(() => {});
    return host;
  }

  async function submit(payload) {
    try {
      setStatus('busy', 'checking...');
      const res = await API.solve(agentName(),
        Object.assign({ token: mission.token, assisted: assistedUsed }, payload));
      showResult(res);
    } catch (err) {
      setStatus('breach', err.message);
      toast(err.message, true);
    }
  }

  /** SUBMIT (by hand, full points) + optional AUTO-SOLVE button (fills the box, half points). */
  function buttonsFor(box, kind, autoFill) {
    const group = [el('button', {
      class: 'btn primary',
      onclick: () => submit(kind === 'cipherText'
        ? { cipherText: box.value }
        : { plainText: box.value })
    }, 'SUBMIT')];

    if (autoFill) {
      group.push(el('button', {
        class: 'btn warn',
        onclick: async () => {
          try {
            assistedUsed = true;
            await autoFill(box);
            submit(kind === 'cipherText'
              ? { cipherText: box.value }
              : { plainText: box.value });
          } catch (err) { toast(err.message, true); }
        }
      }, 'AUTO-SOLVE (half pts)'));
    }
    return el('div', { class: 'btn-row' }, group);
  }

  /* ---------- mission cards ---------- */

  function renderMissionCard() {
    const d = mission.data;

    if (mission.type === 'SEAL_INTEL') {
      const answer = answerBox('type the encrypted letters here');
      return el('div', { class: 'panel' },
        header(mission.type),
        row('MESSAGE', mono(d.message, 'neon')),
        row('KEYWORD', mono(d.keyword)),
        row('THE GRID', gridBlock(d.keyword)),
        row('LETTER PAIRS', mono(d.bigrams, 'dim')),
        row('YOUR ENCRYPTED ANSWER', answer),
        buttonsFor(answer, 'cipherText',
          async (box) => {
            const out = await API.playfairSeal(d.message, d.keyword.toLowerCase());
            box.value = out.cipherText;
          }));
    }

    if (mission.type === 'CRACK_BROADCAST') {
      const answer = answerBox('type the secret message here');
      return el('div', { class: 'panel' },
        header(mission.type),
        row('ENCRYPTED MESSAGE', mono(d.cipherText, 'neon')),
        row('KEYWORD', mono(d.keyword)),
        row('THE GRID', gridBlock(d.keyword)),
        el('p', { class: 'hint', text: 'Tip: stray X letters inside your answer are fine.' }),
        row('YOUR DECRYPTED ANSWER', answer),
        buttonsFor(answer, 'plainText',
          async (box) => {
            const out = await API.playfairOpen(d.cipherTextCompact, d.keyword.toLowerCase());
            box.value = out.plainText;
          }));
    }

    if (mission.type === 'TAMPER_HUNT') {
      let picked = null;
      const table = el('table', { class: 'tactical' },
        el('thead', {}, el('tr', {},
          el('th', { text: 'PICK THE FAKE' }), el('th', { text: 'ID' }),
          el('th', { text: 'PAYLOAD' }), el('th', { text: 'SEAL' }), el('th', { text: 'HASH CHECK' }))),
        el('tbody'));

      d.packages.forEach((pkg) => {
        const radio = el('input', {
          type: 'radio', name: 'fake',
          onchange: () => { picked = pkg.id; }
        });
        const cell = el('td', {}, el('button', {
          class: 'btn ghost',
          onclick: async (ev) => {
            ev.target.disabled = true;
            const out = await API.sha256(pkg.payload + '|' + pkg.keyBlob);
            const ok = out.digest === pkg.seal;
            cell.innerHTML = '';
            cell.append(
              el('span', { class: 'dot ' + (ok ? 'green' : 'red') }),
              document.createTextNode(ok ? 'INTACT' : 'ALTERED!'));
            setStatus(ok ? 'secure' : 'breach',
              'package #' + pkg.id + ': ' + (ok ? 'intact' : 'ALTERED'));
          }
        }, 'HASH'));
        table.querySelector('tbody').append(el('tr', {},
          el('td', {}, radio),
          el('td', { text: '#' + pkg.id }),
          el('td', { class: 'trunc', title: pkg.payload, text: pkg.payload }),
          el('td', { class: 'trunc', title: pkg.seal, text: short(pkg.seal, 12, 8) }),
          cell));
      });

      return el('div', { class: 'panel' },
        header(mission.type),
        row('RULE', el('span', { class: 'hint', text: d.formula + '. Press HASH on all three, then pick the ALTERED one.' })),
        table,
        el('div', { class: 'btn-row' },
          el('button', {
            class: 'btn primary',
            onclick: () => {
              if (picked === null) { toast('Pick one package first', true); return; }
              submit({ flaggedTamperedIds: [picked] });
            }
          }, 'SUBMIT')));
    }

    // SECRET_DROP - RSA unlock, then Playfair decrypt
    const keywordBox = el('input', { type: 'text', placeholder: 'press UNLOCK to reveal', spellcheck: 'false' });
    const answer = answerBox('type the secret message here');
    const gridHost = el('div');
    const hintLine = el('p', { class: 'hint', text: 'Your private key (kept only in this browser) opens the lock.' });

    const unlockBtn = el('button', {
      class: 'btn primary',
      onclick: async (ev) => {
        try {
          ev.target.disabled = true;
          const keyword = await Badge.unlock(d.lockedKeyword, agentName());
          keywordBox.value = keyword.toUpperCase();
          gridHost.replaceWith(gridBlock(keyword));
          hintLine.textContent = 'Keyword unlocked. Now decrypt the message above.';
          setStatus('secure', 'keyword unlocked with your badge');
        } catch (err) {
          ev.target.disabled = false;
          toast(err.message, true);
        }
      }
    }, 'UNLOCK KEYWORD');

    return el('div', { class: 'panel' },
      header(mission.type),
      row('LOCKED KEYWORD', el('div', {},
        el('div', { class: 'mono-block dim', text: short(d.lockedKeyword, 60, 40) }),
        el('div', { style: 'margin-top:8px' }, unlockBtn))),
      row('HOW', hintLine),
      row('ENCRYPTED MESSAGE', mono(d.cipherText, 'neon')),
      row('UNLOCKED KEYWORD', keywordBox),
      row('THE GRID', gridHost),
      row('YOUR DECRYPTED ANSWER', answer),
      el('div', { class: 'btn-row' },
        el('button', {
          class: 'btn warn',
          onclick: async () => {
            if (!keywordBox.value.trim()) { toast('Unlock the keyword first', true); return; }
            assistedUsed = true;
            const out = await API.playfairOpen(d.cipherTextCompact, keywordBox.value.trim());
            answer.value = out.plainText;
            submit({ plainText: out.plainText });
          }
        }, 'AUTO-SOLVE (half pts)'),
        el('button', {
          class: 'btn primary',
          onclick: () => submit({ plainText: answer.value })
        }, 'SUBMIT')));
  }

  /* ---------- results ---------- */

  function showResult(res) {
    profile.score = res.totalScore;
    profile.missionsSolved = res.missionsSolved;
    paintChip();

    const area = $('#play-area');
    area.innerHTML = '';

    area.append(el('div', { class: 'panel', style: 'text-align:center;padding:30px 16px' },
      el('div', {
        style: 'font-size:26px;margin-bottom:14px;color:' + (res.correct ? 'var(--neon)' : 'var(--danger)'),
        text: res.correct ? '[ MISSION COMPLETE ]' : '[ MISSION FAILED ]'
      }),
      el('div', { class: 'result ' + (res.correct ? 'ok' : 'bad'), style: 'text-align:left' },
        el('div', { class: 'headline', text: res.correct ? '+' + res.pointsAwarded + ' PTS - CORRECT!' : 'WRONG' }),
        el('div', { class: 'sub', text: res.correct
          ? 'Nice work, agent.'
          : 'The right answer was: ' + res.expectedAnswer })),
      el('div', { class: 'btn-row', style: 'justify-content:center;margin-top:18px' },
        el('button', {
          class: 'btn primary',
          style: 'font-size:16px;padding:12px 28px',
          onclick: () => { mission = null; renderPlay(); }
        }, 'NEXT MISSION \u00bb'))));

    setStatus(res.correct ? 'secure' : 'breach',
      res.correct ? '+' + res.pointsAwarded + ' pts - total ' + res.totalScore : 'answer revealed - go again');
    area.scrollIntoView({ behavior: 'smooth' });
  }

  /* ================= top agents ================= */

  async function renderTop() {
    const host = $('#top-body');
    try {
      const rows = await API.leaderboard();
      host.innerHTML = '';
      if (!rows.length) { host.append(el('p', { class: 'hint', text: 'No agents yet.' })); return; }
      const me = agentName();
      const table = el('table', { class: 'tactical' },
        el('thead', {}, el('tr', {},
          el('th', { text: '#' }), el('th', { text: 'CODENAME' }),
          el('th', { text: 'SCORE' }), el('th', { text: 'SOLVED' }))),
        el('tbody'));
      rows.forEach((r) => {
        table.querySelector('tbody').append(el('tr', { class: me === r.codename ? 'lb-self' : '' },
          el('td', { text: String(r.position) }),
          el('td', { text: r.codename, style: 'color:var(--neon)' }),
          el('td', { text: String(r.score) }),
          el('td', { text: String(r.missionsSolved) })));
      });
      host.append(table);
    } catch (err) {
      toast(err.message, true);
    }
  }

  /* ================= wire chrome & boot ================= */

  $('#btn-start').addEventListener('click', startGame);
  $('#codename-input').addEventListener('keydown', (ev) => { if (ev.key === 'Enter') startGame(); });
  $('#btn-logout').addEventListener('click', exitGame);

  boot();
})();
