export async function GET(request, { params }) {
  return handleProxy(request, params);
}

export async function POST(request, { params }) {
  return handleProxy(request, params);
}

export async function PUT(request, { params }) {
  return handleProxy(request, params);
}

export async function DELETE(request, { params }) {
  return handleProxy(request, params);
}

async function handleProxy(request, params) {
  const path = Array.isArray(params.path) ? params.path.join('/') : params.path;
  const backendUrl = process.env.BACKEND_URL || 'http://127.0.0.1:8001';
  const url = new URL(request.url);
  const targetUrl = `${backendUrl}/api/${path}${url.search}`;

  const headers = new Headers();
  headers.set('Content-Type', request.headers.get('Content-Type') || 'application/json');

  let body = null;
  if (['POST', 'PUT', 'PATCH'].includes(request.method)) {
    try {
      body = await request.text();
    } catch (e) {
      body = null;
    }
  }

  try {
    const res = await fetch(targetUrl, {
      method: request.method,
      headers: headers,
      body: body,
      cache: 'no-store'
    });

    const data = await res.text();
    return new Response(data, {
      status: res.status,
      headers: {
        'Content-Type': res.headers.get('Content-Type') || 'application/json',
      },
    });
  } catch (err) {
    return new Response(
      JSON.stringify({
        error: "500 Internal Server Error",
        message: `Failed to connect to backend at ${backendUrl}: ${err.message}`,
      }),
      {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      }
    );
  }
}
