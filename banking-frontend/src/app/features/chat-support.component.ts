import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ChatService } from '../core/services/chat.service';

export type Intent = 'BLOCK_CARD' | 'REPLACE_CARD' | 'PHONE_SUPPORT' | 'TECH_SUPPORT' | 'OTHER';

export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  ts: string;
}

@Component({
  selector: 'app-chat-support',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './chat-support.component.html',
  styleUrls: ['./chat-support.component.scss']
})
export class ChatSupportComponent {
  private fb          = inject(FormBuilder);
  private chatService = inject(ChatService);

  msgs = signal<ChatMessage[]>([
    {
      role: 'assistant',
      text: 'Bonjour 👋 Je suis votre assistant. Que puis-je faire pour vous ?',
      ts: new Date().toISOString()
    }
  ]);

  busy  = signal(false);
  error = signal<string | null>(null);

  chatForm = this.fb.group({
    text: ['', [Validators.required, Validators.minLength(2)]],
  });

  intent: Intent | null = null;
  intentForm = this.fb.group({
    numCompte:    [''],
    numeroCarte:  [''],
    phone:        [''],
    email:        [''],
    details:      [''],
  });

  // ── Actions rapides ───────────────────────────────────────────

  setIntent(i: Intent) {
    this.intent = i;
    this.error.set(null);
  }

  sendQuick(i: Intent) {
    this.pushUser(`Je souhaite: ${this.intentLabel(i)}`);
    this.intent = i;
  }

  // ── Formulaire intent ─────────────────────────────────────────

  submitIntent() {
    if (!this.intent) return;
    this.busy.set(true);
    this.error.set(null);

    const v       = this.intentForm.value;
    const contenu = [
      `[${this.intentLabel(this.intent)}]`,
      v.details    ? `Détails: ${v.details}`       : null,
      v.numCompte  ? `Compte: ${v.numCompte}`       : null,
      v.numeroCarte? `Carte: ${v.numeroCarte}`      : null,
      v.phone      ? `Tél: ${v.phone}`              : null,
      v.email      ? `Email: ${v.email}`            : null,
    ].filter(Boolean).join(' | ');

    this.chatService.create(contenu).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.pushAssistant(
          `✅ Demande enregistrée (ticket #${res.id}). Un conseiller va vous contacter rapidement.`
        );
        this.intent = null;
        this.intentForm.reset();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'Erreur lors de l\'envoi de la demande.');
      }
    });
  }

  // ── Envoi message libre ───────────────────────────────────────

  sendText() {
    if (this.chatForm.invalid) {
      this.chatForm.markAllAsTouched();
      return;
    }

    const txt = this.chatForm.value.text!.trim();
    this.chatForm.reset();
    this.pushUser(txt);

    this.busy.set(true);
    this.error.set(null);

    this.chatService.create(txt).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.pushAssistant(
          res.reponse
            ? res.reponse
            : `✅ Message reçu (ticket #${res.id}). Un conseiller va vous répondre sous peu.`
        );
      },
      error: () => {
        this.busy.set(false);
        this.pushAssistant('Désolé, le service est indisponible. Un humain va reprendre la conversation.');
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────

  private pushUser(text: string) {
    this.msgs.update(list => [...list, { role: 'user', text, ts: new Date().toISOString() }]);
  }

  private pushAssistant(text: string) {
    this.msgs.update(list => [...list, { role: 'assistant', text, ts: new Date().toISOString() }]);
  }

  intentLabel(i: Intent): string {
    switch (i) {
      case 'BLOCK_CARD':    return 'Bloquer ma carte';
      case 'REPLACE_CARD':  return 'Remplacer ma carte';
      case 'PHONE_SUPPORT': return 'Être rappelé par téléphone';
      case 'TECH_SUPPORT':  return 'Assistance technique';
      default:              return 'Autre demande';
    }
  }
}
