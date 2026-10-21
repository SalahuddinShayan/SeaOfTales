import { createElement } from "react";

type NovelDescriptionProps = {
  description: string | null;
};

const fallbackDescription = "A new story is waiting to be discovered across the sea.";

function toPlainText(description: string) {
  return description
    .replace(/<!--[\s\S]*?-->/g, "")
    .replace(/<\s*(script|style|iframe|object|template|svg|math)\b[^>]*>[\s\S]*?(?:<\s*\/\s*\1\s*>|$)/gi, "")
    .replace(/<\s*br\s*\/?\s*>/gi, "\n")
    .replace(/<\s*\/\s*(p|div|li|h[1-6]|blockquote)\s*>/gi, "\n\n")
    .replace(/<\s*(p|div|li|h[1-6]|blockquote)\b[^>]*>/gi, "\n\n")
    .replace(/<\/?[a-z][^>]*>/gi, "")
    .replace(/<\s*\/?\s*[a-z][^<>]*$/gi, "")
    .replace(/[ \t]*\r?\n[ \t]*/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

export default function NovelDescription({ description }: NovelDescriptionProps) {
  return createElement(
    "div",
    { className: "novel-description" },
    description ? toPlainText(description) || fallbackDescription : fallbackDescription,
  );
}
