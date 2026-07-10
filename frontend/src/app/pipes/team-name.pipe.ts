import { Pipe, PipeTransform } from '@angular/core';
import { TEAM_TRANSLATIONS } from './team-translations';

@Pipe({ name: 'teamName', standalone: true })
export class TeamNamePipe implements PipeTransform {
  transform(value: string | null | undefined, lang: 'en' | 'pt' = 'pt'): string {
    if (value == null || value === '') return '';
    if (lang === 'en') return value;
    return TEAM_TRANSLATIONS[value] ?? value;
  }
}