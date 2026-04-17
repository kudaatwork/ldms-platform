use actix_web::HttpResponse;

pub async fn ui() -> HttpResponse {
    HttpResponse::Ok()
        .content_type("text/html; charset=utf-8")
        .body(
            r#"<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>LDMS Rust FS UI</title>
  <style>
    body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; margin: 24px; max-width: 980px; }
    h1 { margin-bottom: 8px; }
    .hint { color: #555; margin-top: 0; }
    .card { border: 1px solid #ddd; border-radius: 8px; padding: 14px; margin: 12px 0; }
    .row { display: flex; gap: 10px; flex-wrap: wrap; margin: 8px 0; }
    input, button, select { padding: 8px; font-size: 14px; }
    input { min-width: 220px; }
    button { cursor: pointer; }
    pre { background: #111; color: #ddd; padding: 10px; border-radius: 6px; overflow-x: auto; }
    .ok { color: #0a7c2f; font-weight: 600; }
    .err { color: #b00020; font-weight: 600; }
  </style>
</head>
<body>
  <h1>LDMS Rust File Service UI</h1>
  <p class="hint">This UI calls the same protected APIs under <code>/health</code> and <code>/files/*</code>.</p>

  <div class="card">
    <h3>Authentication Header</h3>
    <div class="row">
      <input id="token" type="password" placeholder="X-Internal-Token value" />
      <button onclick="setExampleToken()">Use dev default</button>
    </div>
    <small>Header sent on every API request: <code>X-Internal-Token: ...</code></small>
  </div>

  <div class="card">
    <h3>Health Check</h3>
    <button onclick="checkHealth()">GET /health</button>
  </div>

  <div class="card">
    <h3>Upload File</h3>
    <div class="row">
      <input id="orgId" placeholder="org_id (e.g. acme)" value="acme" />
      <input id="refType" placeholder="ref_type (e.g. invoice)" value="invoice" />
      <input id="fileInput" type="file" />
    </div>
    <button onclick="upload()">POST /files/upload</button>
  </div>

  <div class="card">
    <h3>File Operations</h3>
    <div class="row">
      <input id="opOrgId" placeholder="org_id" value="acme" />
      <input id="opRefType" placeholder="ref_type" value="invoice" />
      <input id="fileId" placeholder="file_id" />
    </div>
    <div class="row">
      <button onclick="metadata()">GET metadata</button>
      <button onclick="downloadFile()">GET file</button>
      <button onclick="softDelete()">DELETE file</button>
    </div>
  </div>

  <h3>Response</h3>
  <div id="status"></div>
  <pre id="out">Ready.</pre>

  <script>
    function token() {
      return document.getElementById('token').value.trim();
    }

    function headers(extra) {
      const h = new Headers(extra || {});
      const t = token();
      if (t) h.set('X-Internal-Token', t);
      return h;
    }

    function showStatus(ok, msg) {
      const el = document.getElementById('status');
      el.className = ok ? 'ok' : 'err';
      el.textContent = msg;
    }

    function show(data) {
      const out = document.getElementById('out');
      out.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
    }

    function setExampleToken() {
      document.getElementById('token').value = 'dev-token-change-me';
    }

    async function callApi(url, options) {
      try {
        const res = await fetch(url, options);
        const ct = res.headers.get('content-type') || '';
        let body;
        if (ct.includes('application/json')) {
          body = await res.json();
        } else {
          body = await res.text();
        }
        showStatus(res.ok, `${res.status} ${res.statusText}`);
        show(body);
        return {res, body};
      } catch (e) {
        showStatus(false, 'Network error');
        show(String(e));
        return null;
      }
    }

    async function checkHealth() {
      await callApi('/health', { method: 'GET', headers: headers() });
    }

    async function upload() {
      const orgId = document.getElementById('orgId').value.trim();
      const refType = document.getElementById('refType').value.trim();
      const file = document.getElementById('fileInput').files[0];
      if (!orgId || !refType || !file) {
        showStatus(false, 'Missing required fields');
        show('Provide org_id, ref_type, and a file.');
        return;
      }

      const form = new FormData();
      form.append('org_id', orgId);
      form.append('ref_type', refType);
      form.append('file', file);

      await callApi('/files/upload', { method: 'POST', headers: headers(), body: form });
    }

    function ids() {
      const orgId = document.getElementById('opOrgId').value.trim();
      const refType = document.getElementById('opRefType').value.trim();
      const fileId = document.getElementById('fileId').value.trim();
      if (!orgId || !refType || !fileId) return null;
      return { orgId, refType, fileId };
    }

    async function metadata() {
      const v = ids();
      if (!v) return show('Set org_id, ref_type, and file_id first.');
      await callApi(`/files/metadata/${v.orgId}/${v.refType}/${v.fileId}`, { method: 'GET', headers: headers() });
    }

    async function downloadFile() {
      const v = ids();
      if (!v) return show('Set org_id, ref_type, and file_id first.');
      const r = await callApi(`/files/${v.orgId}/${v.refType}/${v.fileId}`, { method: 'GET', headers: headers() });
      if (r && r.res.ok) {
        show('Download started (or browser preview opened, depending on file type).');
        const blob = await r.res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = v.fileId;
        a.click();
        URL.revokeObjectURL(url);
      }
    }

    async function softDelete() {
      const v = ids();
      if (!v) return show('Set org_id, ref_type, and file_id first.');
      await callApi(`/files/${v.orgId}/${v.refType}/${v.fileId}`, { method: 'DELETE', headers: headers() });
    }
  </script>
</body>
</html>
"#,
        )
}
