import type { Metadata } from 'next';
import { Providers } from '@/components/providers';
import './globals.css';

export const metadata: Metadata = {
  title: 'E-Vault — Secure Digital Evidence Management',
  description:
    'Chain-of-custody-grade platform for collecting, storing, analysing, and certifying digital evidence and legal documents. BSA 2023 §63 compliant.',
  keywords: [
    'digital evidence',
    'e-vault',
    'chain of custody',
    'BSA 2023',
    'legal document management',
    'forensic analysis',
  ],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="antialiased min-h-screen">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
