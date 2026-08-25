/* BLACKOUT // api.js - fetch bridge to the relay */
(function () {
  'use strict';

  async function request(method, path, body) {
    const options = { method, headers: { 'Accept': 'application/json' } };
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }
    let response;
    try {
      response = await fetch(path, options);
    } catch (networkError) {
      throw { status: 0, message: 'SERVER UNREACHABLE - check the relay endpoint.' };
    }

    const text = await response.text();
    let payload = null;
    if (text) { try { payload = JSON.parse(text); } catch (e) { /* ignore */ } }

    if (!response.ok) {
      const message = payload && payload.error
        ? payload.error + (payload.detail ? ': ' + payload.detail : '')
        : ('HTTP ' + response.status);
      throw { status: response.status, message };
    }
    return payload;
  }

  window.API = {
    enlist: (codename) => request('POST', '/api/agents', { codename }),
    dossier: (codename) => request('GET', '/api/agents/' + encodeURIComponent(codename)),
    registerBadge: (codename, publicKey) =>
      request('PUT', '/api/agents/' + encodeURIComponent(codename) + '/badge', { publicKey }),
    leaderboard: () => request('GET', '/api/leaderboard'),
    newMission: (codename, type) =>
      request('POST', `/api/missions/${encodeURIComponent(codename)}/new?type=${encodeURIComponent(type)}`),
    solve: (codename, solveRequest) =>
      request('POST', `/api/missions/${encodeURIComponent(codename)}/solve`, solveRequest),
    playfairGrid: (keyword) =>
      request('GET', '/api/tools/playfair/grid?keyword=' + encodeURIComponent(keyword)),
    playfairSeal: (message, keyword) =>
      request('POST', '/api/tools/playfair/seal', { message, keyword }),
    playfairOpen: (cipherText, keyword) =>
      request('POST', '/api/tools/playfair/open', { message: cipherText, keyword }),
    sha256: (input) => request('POST', '/api/tools/sha256', { input })
  };
})();
