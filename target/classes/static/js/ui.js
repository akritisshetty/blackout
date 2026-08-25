/* BLACKOUT // ui.js - tiny DOM helpers, toasts, status bar */
(function () {
  'use strict';

  function el(tag, attrs, ...children) {
    const node = document.createElement(tag);
    if (attrs) {
      for (const [key, value] of Object.entries(attrs)) {
        if (key === 'class') node.className = value;
        else if (key === 'text') node.textContent = value;
        else if (key === 'html') node.innerHTML = value;
        else if (key.startsWith('on') && typeof value === 'function') {
          node.addEventListener(key.slice(2), value);
        } else if (value !== null && value !== undefined) {
          node.setAttribute(key, value);
        }
      }
    }
    for (const child of children.flat()) {
      if (child === null || child === undefined) continue;
      node.append(child.nodeType ? child : document.createTextNode(child));
    }
    return node;
  }

  function $(selector, root) { return (root || document).querySelector(selector); }
  function $all(selector, root) { return Array.from((root || document).querySelectorAll(selector)); }

  function toast(message, isError) {
    const wrap = $('#toasts');
    if (!wrap) return;
    const item = el('div', { class: 'toast' + (isError ? ' err' : ''), text: message });
    wrap.append(item);
    setTimeout(() => item.remove(), 4200);
  }

  function setStatus(state, message) {
    const pill = $('#status-pill');
    const msg = $('#status-msg');
    if (!pill) return;
    pill.classList.remove('secure', 'breach', 'busy');
    const labels = {
      idle: '[ STATUS: IDLE ]',
      secure: '[ STATUS: SECURE ]',
      breach: '[ STATUS: COMPROMISED ]',
      busy: '[ STATUS: WORKING ]'
    };
    pill.textContent = labels[state] || labels.idle;
    if (state === 'secure') pill.classList.add('secure');
    if (state === 'breach') pill.classList.add('breach');
    if (state === 'busy') pill.classList.add('busy');
    if (message !== undefined && msg) msg.textContent = message;
  }

  function short(value, head, tail) {
    const s = String(value == null ? '' : value);
    if (s.length <= head + tail + 1) return s;
    return s.slice(0, head) + '\u2026' + s.slice(s.length - tail);
  }

  window.UI = { el, $, $all, toast, setStatus, short };
})();
