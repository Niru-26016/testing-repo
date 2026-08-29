# ArgusStore: Next.js & FastAPI E-Commerce Target for ARGUS

> "A real-world e-commerce application designed to demonstrate ARGUS Autonomous Closed-Loop Debugging in production."

---

## Technical Architecture

- **Frontend:** Next.js (App Router) + React 18 + Tailwind CSS + Lucide Icons (Port 3000)
- **Backend API:** FastAPI Microservices Architecture (API Gateway on Port 8001, Pricing Service on 8002, Shipping Service on 8003, Inventory Service on 8004)
- **Production Observer:** Built-in `argus_observer_middleware` intercepting unhandled 500 exceptions, scrubbing sensitive data, and posting telemetry to ARGUS `/webhook/crash`.
- **Containerization:** Multi-stage Docker setup with Docker Compose (`docker-compose.yml`)

---

## Repository Structure

```
testing-repo/
├── frontend/                     # Next.js Storefront Application
│   ├── Dockerfile                # Multi-stage container build
│   ├── package.json              # Next.js & UI dependencies
│   ├── next.config.mjs           # API rewrites & standalone output
│   ├── tailwind.config.js        # Tailwind styling
│   ├── jsconfig.json             # Path alias mapping
│   └── src/
│       └── app/
│           ├── layout.jsx        # Root HTML shell
│           ├── page.jsx          # Interactive storefront & cart drawer
│           └── globals.css       # Tailwind base directives
│
├── backend/                      # Python FastAPI Microservices Backend
│   ├── Dockerfile                # Python microservices container
│   ├── index.py                  # Microservices API Gateway & Observer Middleware
│   ├── run_microservices.py      # Multi-service runner (Ports 8001-8004)
│   ├── requirements.txt          # Backend dependencies
│   ├── microservices/            # Domain microservices
│   │   ├── pricing_service.py    # Pricing & discount logic (ZeroDivision & KeyError bugs)
│   │   ├── shipping_service.py   # Shipping rate calculations (KeyError bug)
│   │   └── inventory_service.py  # Inventory validation (IndexError bug)
│   ├── services/                 # Business logic engines
│   │   ├── pricing_engine.py
│   │   ├── shipping_engine.py
│   │   └── inventory_engine.py
│   └── tests/                    # PyTest test suite for ARGUS Sandbox Gate
│       ├── test_pricing.py
│       └── test_shipping.py
│
├── docker-compose.yml            # Orchestrates frontend (:3000) and backend (:8001-:8004)
├── package.json                  # Root monorepo scripts
└── README.md
```

---

## Running with Docker (Recommended)

To build and launch both the Next.js frontend and Python FastAPI backend microservices simultaneously:

```bash
docker compose up --build
```

- **Next.js Frontend:** [http://localhost:3000](http://localhost:3000)
- **API Gateway:** [http://localhost:8001](http://localhost:8001)
- **Pricing Microservice:** [http://localhost:8002](http://localhost:8002)
- **Shipping Microservice:** [http://localhost:8003](http://localhost:8003)
- **Inventory Microservice:** [http://localhost:8004](http://localhost:8004)

To shut down containers:
```bash
docker compose down
```

---

## Local Development (Without Docker)

### 1. Install & Run Python Backend Microservices

```bash
cd backend
pip install -r requirements.txt
python run_microservices.py
```

### 2. Install & Run Next.js Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## Root Monorepo Convenience Scripts

From the repository root:
- `npm run dev`: Starts the Next.js frontend development server.
- `npm run build`: Builds the Next.js frontend for production.
- `npm run backend`: Runs all Python microservices locally.
- `npm run docker:up`: Starts all services with Docker Compose.
- `npm run docker:down`: Stops all Docker Compose containers.

---

## How to Trigger Production Crashes for ARGUS

1. **Trigger `KeyError` (Invalid Promo Code):**
   - Add any product to your cart.
   - Open **Cart** $\rightarrow$ Enter promo code `INVALID50`.
   - Click **Place Order & Calculate**.
   - Throws `KeyError: 'INVALID50'` in `pricing_service.py`. Observer intercepts and posts telemetry to ARGUS.

2. **Trigger `ZeroDivisionError` (100% Promo Code):**
   - Add any product to your cart.
   - Open **Cart** $\rightarrow$ Enter promo code `FREESHIP100`.
   - Click **Place Order & Calculate**.
   - Throws `ZeroDivisionError` in `pricing_service.py`. Observer intercepts and posts telemetry to ARGUS.

3. **Trigger `KeyError` (Missing Address Field):**
   - Open **Cart** $\rightarrow$ Clear the Zip Code input in the address form.
   - Click **Place Order & Calculate**.
   - Throws `KeyError: 'zip_code'` in `shipping_service.py`.

4. **Trigger `IndexError` (Empty Cart Check):**
   - Submit checkout request with an empty items array.
   - Throws `IndexError: list index out of range` in `inventory_service.py`.
