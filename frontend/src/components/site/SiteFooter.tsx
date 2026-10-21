import Link from "next/link";

export default function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="site-footer__inner">
        <Link href="/" className="site-footer__brand">SEA OF TALES</Link>
        <p>Stories carried across the sea.</p>
        <Link href="/about">About</Link>
        <Link href="/authors">Authors</Link>
        <Link href="/contact">Contact</Link>
        <Link href="/faq">FAQ / Help</Link>
        <Link href="/terms">Terms</Link>
        <Link href="/copyright">Copyright / Rights</Link>
        <Link href="/content-policy">Content Policy</Link>
        <Link href="/#featured">Back to the surface ↑</Link>
      </div>
    </footer>
  );
}
