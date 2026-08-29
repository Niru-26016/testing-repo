# ArgusStore: Dummy E-Commerce Website for ARGUS Production Observer

> "A real-world e-commerce application designed to demonstrate ARGUS Autonomous Closed-Loop Debugging in production."

---

## Technical Architecture

- **Frontend:** React 18 + Vite + Tailwind CSS + Lucide Icons (Port 5173)
- **Backend API:** FastAPI Microservices Architecture (API Gateway on Port 8001, Pricing Service on 8002, Shipping Service on 8003, Inventory Service on 8004)
- **Production Observer:** Built-in `argus_observer_middleware` intercepting unhandled 500 exceptions, scrubbing sensitive data, and posting telemetry to ARGUS `/webhook/crash`.
- **Deployment Platform:** Vercel / Standard Container Deployment

---

## Repository Structure

```
demo_store/
├── package.json          # React & Vite dependencies
├── vite.config.js        # Vite config with dev server on port 5173 & API proxying
├── index.html            # Root HTML entry point
├── api/
│   ├── index.py          # Microservices API Gateway & ArgusObserverMiddleware
│   ├── run_microservices.py # Local runner script for all microservices
│   ├── microservices/    # Independent FastAPI microservices
│   │   ├── pricing_service.py   # Cart & discount calculations (ZeroDivision & KeyError bugs)
│   │   ├── shipping_service.py  # Address & shipping rates (KeyError bug)
│   │   └── inventory_service.py # Stock allocation (IndexError bug)
│   └── tests/            # PyTest suite for ARGUS Sandbox Gate
│       ├── test_pricing.py
│       └── test_shipping.py
└── src/
    ├── main.jsx          # React 18 root entry point
    ├── App.jsx           # Storefront React application & Cart slide-over
    └── index.css         # Tailwind CSS setup
```

---

## Local Development & Testing

### 1. Run Python API Microservices Backend
```bash
python api/run_microservices.py
```
- **API Gateway:** `http://localhost:8001`
- **Pricing Service:** `http://localhost:8002`
- **Shipping Service:** `http://localhost:8003`
- **Inventory Service:** `http://localhost:8004`

### 2. Run React Storefront Frontend
```bash
npm install
npm run dev
```
Open `http://localhost:5174` in your browser.

---

## How to Trigger Production Crashes for ARGUS

1. **Trigger `KeyError` (Invalid Promo Code):**
   - Add any product to your cart.
   - Click **Cart** $\rightarrow$ Enter promo code `INVALID50`.
   - Click **Place Order & Calculate**.
   - Throws `KeyError: 'INVALID50'` in `pricing_service.py`.

2. **Trigger `ZeroDivisionError` (100% Promo Code):**
   - Add any product to your cart.
   - Click **Cart** $\rightarrow$ Enter promo code `FREESHIP100`.
   - Click **Place Order & Calculate**.
   - Throws `ZeroDivisionError` in `pricing_service.py`.

3. **Trigger `KeyError` (Missing Address Field):**
   - Click **Cart** $\rightarrow$ Clear the Zip Code input in the address form.
   - Click **Place Order & Calculate**.
   - Throws `KeyError: 'zip_code'` in `shipping_service.py`.
