"use client";

export type AppLanguage = "ENGLISH" | "JAPANESE" | "KOREAN";

const selectorCopy: Record<AppLanguage, { label: string; options: Record<AppLanguage, string> }> = {
  ENGLISH: {
    label: "Language:",
    options: { ENGLISH: "English", JAPANESE: "Japanese / 日本語", KOREAN: "Korean / 한국어" },
  },
  JAPANESE: {
    label: "言語:",
    options: { ENGLISH: "英語 / English", JAPANESE: "日本語 / Japanese", KOREAN: "韓国語 / Korean" },
  },
  KOREAN: {
    label: "언어:",
    options: { ENGLISH: "영어 / English", JAPANESE: "일본어 / Japanese", KOREAN: "한국어 / Korean" },
  },
};

export default function LanguageSelector({
  id,
  value,
  interfaceLanguage,
  onChange,
  placeholder,
}: {
  id: string;
  value: AppLanguage | "";
  interfaceLanguage: AppLanguage;
  onChange: (language: AppLanguage | "") => void;
  placeholder?: string;
}) {
  const copy = selectorCopy[interfaceLanguage];
  return (
    <div className="flex items-center gap-2">
      <label htmlFor={id} className="text-sm text-white/85">{copy.label}</label>
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value as AppLanguage | "")}
        className="rounded-lg border border-white/20 bg-[#10232d] px-3 py-2 text-sm text-white"
      >
        {placeholder && <option value="">{placeholder}</option>}
        {(Object.keys(copy.options) as AppLanguage[]).map((option) => (
          <option key={option} value={option}>{copy.options[option]}</option>
        ))}
      </select>
    </div>
  );
}
