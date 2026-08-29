import './globals.css';

export const metadata = {
  title: 'ArgusStore | E-Commerce Production Observer Demo',
  description: 'Fully working production e-commerce store instrumented with ARGUS Autonomous Debugger.',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
        {children}
      </body>
    </html>
  );
}
