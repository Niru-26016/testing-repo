# ArgusStore: Dummy E-Commerce Website for ARGUS Production Observer

> "A real-world e-commerce application designed to demonstrate ARGUS Autonomous Closed-Loop Debugging in production."

---

## Technical Architecture

- **Frontend:** Next.js (React 18 + Tailwind CSS + Lucide Icons)
- **Backend API:** FastAPI (Python 3.11 Serverless Functions)
- **Production Observer:** Built-in `argus_observer_middleware` intercepting unhandled 500 exceptions, scrubbing sensitive data, and posting telemetry to ARGUS `/webhook/crash`.
- **Deployment Platform:** Vercel (Unified Next.js + Python Serverless Functions)

---

## Repository Structure

```
demo_store/
├── package.json          # Next.js & React dependencies
├── vercel.json           # Vercel serverless routing (/api/* -> api/index.py)
├── next.config.js        # Next.js API proxy for local development
├── api/
│   ├── index.py          # FastAPI application & ArgusObserverMiddleware
│   ├── requirements.txt  # Python serverless dependencies
│   ├── services/
│   │   ├── pricing_engine.py  # Cart calculation logic (ZeroDivisionError bug)
│   │   ├── shipping_engine.py # Shipping calculation logic (KeyError bug)
│   │   └── inventory_engine.py# Stock allocation logic (IndexError bug)
│   └── tests/                 # PyTest suite for ARGUS Sandbox Gate
│       ├── test_pricing.py
│       └── test_shipping.py
└── src/
    └── app/
        ├── layout.jsx    # Root layout & meta tags
        ├── globals.css   # Tailwind CSS setup
        └── page.jsx      # Interactive Storefront & Checkout Drawer
```

---

## Local Development & Testing

### 1. Run Python API Backend
```bash
cd demo_store
pip install -r api/requirements.txt
uvicorn api.index:app --port 8001 --reload
```

### 2. Run Next.js Storefront Frontend
```bash
cd demo_store
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## How to Trigger Production Crashes for ARGUS

1. **Trigger `ZeroDivisionError`:**
   - Add any product to your cart.
   - Click **Cart** $\rightarrow$ Enter promo code `FREESHIP100`.
   - Click **Place Order & Calculate**.
   - The Python API hits `pricing_engine.py` line 26 and throws a `ZeroDivisionError`.
   - `ArgusObserverMiddleware` catches the crash and posts a webhook to ARGUS!

2. **Trigger `KeyError`:**
   - Click **Cart** $\rightarrow$ Clear the Zip Code field in the shipping address form.
   - Click **Place Order & Calculate**.
   - Throws `KeyError: 'zip_code'` in `shipping_engine.py`.

---

## Single-Click Vercel Deployment

1. Install Vercel CLI or connect your GitHub repository to Vercel:
   ```bash
   vercel
   ```
2. Set Environment Variables in Vercel Dashboard:
   - `ARGUS_WEBHOOK_URL`: `https://your-argus-backend.com/webhook/crash`
3. Click Deploy! Vercel automatically deploys both the Next.js Storefront and Python API Serverless functions.
