/* BLACKOUT // badge.js - automatic RSA badge, minted in the browser
 *
 * The first time an agent plays, this file silently generates an RSA-2048 key pair
 * (WebCrypto, RSA-OAEP + SHA-256). The public half goes to the relay; the private half
 * stays in localStorage and unlocks SECRET DROP missions. No buttons, no management.
 */
(function () {
  'use strict';

  const ALG = { name: 'RSA-OAEP', hash: 'SHA-256', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]) };
  const STORE_PREFIX = 'blackout.badge.';

  function supported() {
    return typeof crypto !== 'undefined' && crypto.subtle !== undefined;
  }

  function b64(buffer) {
    return btoa(String.fromCharCode(...new Uint8Array(buffer)));
  }

  function unb64(base64) {
    const binary = atob(base64.trim());
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes.buffer;
  }

  async function generate() {
    const pair = await crypto.subtle.generateKey(ALG, true, ['encrypt', 'decrypt']);
    return {
      publicKey: b64(await crypto.subtle.exportKey('spki', pair.publicKey)),
      privateKey: b64(await crypto.subtle.exportKey('pkcs8', pair.privateKey))
    };
  }

  /** Makes sure this agent has a badge. Silent - never bothers the player. */
  async function ensure(codename, alreadyRegistered) {
    const key = STORE_PREFIX + codename.toUpperCase();
    let local = null;
    try { local = JSON.parse(localStorage.getItem(key)); } catch (e) { /* ignore */ }
    if (local && local.privateKey && alreadyRegistered) return;
    const pair = await generate();
    localStorage.setItem(key, JSON.stringify(pair));
    await window.API.registerBadge(codename, pair.publicKey);
  }

  /** Unlocks a server-made blob with the local private key. Returns the secret text. */
  async function unlock(rsaBlobB64, codename) {
    let local = null;
    try { local = JSON.parse(localStorage.getItem(STORE_PREFIX + codename.toUpperCase())); } catch (e) { /* ignore */ }
    if (!local || !local.privateKey) throw new Error('No private badge in this browser');
    const key = await crypto.subtle.importKey(
      'pkcs8', unb64(local.privateKey), { name: 'RSA-OAEP', hash: 'SHA-256' }, true, ['decrypt']);
    const plain = await crypto.subtle.decrypt({ name: 'RSA-OAEP' }, key, unb64(rsaBlobB64));
    return new TextDecoder().decode(plain);
  }

  window.Badge = { supported, ensure, unlock };
})();
