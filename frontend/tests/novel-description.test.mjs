import assert from "node:assert/strict";
import test from "node:test";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import NovelDescription from "../src/components/NovelDescription.ts";

test("converts description formatting tags to readable text", () => {
  const description = "<p>First paragraph.</p><p>Second<br>line.</p><p><em>Emphasized</em></p>";
  const markup = renderToStaticMarkup(
    createElement(NovelDescription, { description }),
  );

  assert.match(markup, /First paragraph\.\n\nSecond\nline\.\n\nEmphasized/);
  assert.doesNotMatch(markup, /&lt;\/?p&gt;|&lt;br&gt;|&lt;em&gt;/);
});

test("removes scripts and event-handler markup without rendering active elements", () => {
  const maliciousDescription = '<p>Safe</p><script>alert(2)</script><img src=x onerror="alert(1)">';
  const markup = renderToStaticMarkup(
    createElement(NovelDescription, { description: maliciousDescription }),
  );

  assert.match(markup, /Safe/);
  assert.doesNotMatch(markup, /alert|<script\b|<img\b|onerror/);
});

test("preserves plain-text descriptions and their line breaks", () => {
  const description = "First paragraph.\n\nSecond paragraph.";
  const markup = renderToStaticMarkup(
    createElement(NovelDescription, { description }),
  );

  assert.match(markup, /First paragraph\.\n\nSecond paragraph\./);
});

test("handles malformed tags and empty descriptions", () => {
  const malformedMarkup = renderToStaticMarkup(
    createElement(NovelDescription, { description: "First<p>Second<br>Third<script>unfinished" }),
  );
  const emptyDescription = renderToStaticMarkup(
    createElement(NovelDescription, { description: " <p></p> " }),
  );

  assert.match(malformedMarkup, /First\n\nSecond\nThird/);
  assert.doesNotMatch(malformedMarkup, /unfinished|&lt;p|&lt;br/);
  assert.match(emptyDescription, /A new story is waiting to be discovered across the sea\./);
});
