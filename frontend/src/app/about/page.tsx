import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

export default function AboutPage() {
  return (
    <div className="min-h-screen">
      <SiteNavbar />
      <main className="about-page">
        <header className="about-page__hero">
          <p className="home-eyebrow">Stories carried across the sea</p>
          <h1>About Sea of Tales</h1>
          <h2>Stories Have No Language Barriers</h2>
          <p>Every novel begins with an idea, a world, and a story worth telling. But language can determine how far that story travels and who gets the chance to experience it.</p>
          <p><strong>Sea of Tales exists to bring stories across language barriers and connect novels with readers around the world through authorized translations.</strong></p>
          <p>We believe readers should be able to discover captivating stories from different cultures, while authors and publishers should have a meaningful role in how their work is translated, shared, and enjoyed by a wider audience.</p>
        </header>

        <div className="about-page__sections">
          <section>
            <h2>Our Mission</h2>
            <p>Our mission is to make novels from different languages accessible to a global readership through a platform built around authorized translations, thoughtful editorial work, and respect for the people who create these stories.</p>
            <p>We want to build a place where readers can discover their next favorite novel, authors can reach readers beyond their original language, and translation teams can help stories find new audiences.</p>
          </section>

          <section>
            <h2>What We Do</h2>
            <div className="about-page__subsections">
              <article><h3>Discover Stories from Different Worlds</h3><p>Sea of Tales brings together novels for readers who enjoy exploring new worlds, characters, cultures, and ideas. Our goal is to make discovering and following stories straightforward, with a reading experience designed around the novels themselves.</p></article>
              <article><h3>Bring Novels Across Languages</h3><p>Translation is more than replacing words from one language with another. It is about communicating meaning, preserving the author&apos;s intent, and helping readers experience a story as naturally as possible.</p><p>Our translation workflow combines translation preparation, terminology consistency, and editorial review to help produce clear, consistent English editions while respecting the original work.</p></article>
              <article><h3>Build with Authors and Publishers</h3><p>Authors and publishers are essential to the stories we share. Sea of Tales is being developed around an authorized publishing and translation model, with the aim of working with rights holders to bring their novels to new audiences.</p><p>We believe that the people who create stories deserve recognition and respect for their work. We intend to develop sustainable arrangements that allow authorized novels to reach readers while supporting the people behind them.</p></article>
            </div>
          </section>

          <section><h2>For Readers</h2><p>Whether you are looking for your next long-running adventure or a completed story to enjoy at your own pace, Sea of Tales aims to make discovering and reading novels simple and enjoyable.</p><p>Our focus is on a comfortable reading experience, easy access to novel and chapter information, and a growing collection of translated stories.</p></section>

          <section><h2>For Authors and Publishers</h2><p>Sea of Tales aims to help authors and publishers reach readers who might otherwise never encounter their work because of language barriers.</p><p>We welcome enquiries from rights holders interested in discussing translation, licensing, and publication opportunities. Any specific project or partnership would need to be discussed and agreed upon with the relevant rights holders.</p></section>

          <section><h2>Our Approach to Translation</h2><p>We treat translation as a process that requires consistency, care, and editorial attention.</p><p>Our workflow is designed to support terminology management, translation preparation, and human editing. Technology can assist this process, but the goal remains a readable and consistent translation that respects the source material.</p><p>We also recognize that every novel has its own voice, terminology, and storytelling style. Preserving those qualities is an important part of making a translated work enjoyable.</p></section>

          <section><h2>Looking Ahead</h2><p>Sea of Tales is being built step by step, with the goal of growing into a sustainable platform for authorized novel translations.</p><p>As the platform develops, we intend to expand its catalogue, strengthen relationships with authors and publishers, improve the reading experience, and introduce ways for readers to support the people behind the stories.</p><p>We know that building a trustworthy platform takes time. Our aim is to grow thoughtfully, respect the rights behind every story, and create something readers and creators can value for years to come.</p></section>
        </div>

        <section className="about-page__closing"><h2>Every Story Deserves the Chance to Travel</h2><p>A language barrier should not be the only thing standing between a reader and a story they might love.</p><p><strong>Welcome to Sea of Tales.</strong></p></section>
      </main>
      <SiteFooter />
    </div>
  );
}
