import de from '../../../public/i18n/de.json';
import en from '../../../public/i18n/en.json';
import { detectLanguage } from './language';

describe('detectLanguage', () => {
  it('takes the first preferred language servdesk speaks', () => {
    expect(detectLanguage(['fr-FR', 'de-CH', 'en-US'])).toBe('de');
  });

  it('matches on the primary subtag, case-insensitively', () => {
    expect(detectLanguage(['DE-at'])).toBe('de');
    expect(detectLanguage(['en-GB'])).toBe('en');
  });

  it('falls back to English', () => {
    expect(detectLanguage(['fr', 'it'])).toBe('en');
    expect(detectLanguage([])).toBe('en');
  });
});

describe('translations', () => {
  function keys(node: object, prefix = ''): string[] {
    return Object.entries(node).flatMap(([key, value]) =>
      typeof value === 'object' && value !== null
        ? keys(value, `${prefix}${key}.`)
        : [`${prefix}${key}`],
    );
  }

  it('German and English define exactly the same keys', () => {
    expect(keys(de).sort()).toEqual(keys(en).sort());
  });
});
