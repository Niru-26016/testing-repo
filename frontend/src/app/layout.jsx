import './globals.css';

export const metadata = {
  title: 'ArgusStore - Developer E-Commerce Demo Store',
  description: 'Next.js E-Commerce Target App for Argus Autonomous Production Observer',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className="antialiased bg-slate-950 text-slate-100 min-h-screen">
        {children}
      </body>
    </html>
  );
}
