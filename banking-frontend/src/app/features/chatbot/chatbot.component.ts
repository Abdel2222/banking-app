import { Component, signal, OnInit, OnDestroy, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject } from 'rxjs';

import { BankAccount } from '../../core/services/accounts.service';
import { Operation } from '../../core/services/operations.service';
import { ChatService, CreateChatPayload } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';

interface ChatMessage {
  id: string;
  text: string;
  isBot: boolean;
  timestamp: Date;
}

interface QuickAction {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.scss']
})
export class ChatbotComponent implements OnInit, OnDestroy {
  @Input() selectedAccount: BankAccount | null = null;
  @Input() operations: Operation[] = [];
  @Input() userName: string | null = null;

  private fb          = inject(FormBuilder);
  private chatService = inject(ChatService);
  private auth        = inject(AuthService);
  private destroy$    = new Subject<void>();

  isExpanded        = signal(false);
  isTyping          = signal(false);
  messages          = signal<ChatMessage[]>([]);
  hasUnreadMessages = signal(false);

  quickActions = signal<QuickAction[]>([
    { label: 'Problème avec mon compte', icon: '🏦' },
    { label: 'Bloquer ma carte',         icon: '💳' },
    { label: 'Problème de virement',     icon: '💸' },
    { label: 'Assistance technique',     icon: '🔧' },
  ]);

  messageForm = this.fb.group({
    message: ['', [Validators.required, Validators.minLength(1)]]
  });

  ngOnInit() {
    this.messages.set([{
      id: this.generateId(),
      text: `Bonjour ${this.userName ?? ''} 👋 Je suis votre assistant. Comment puis-je vous aider ?`,
      isBot: true,
      timestamp: new Date()
    }]);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleChat() {
    this.isExpanded.update(v => !v);
    if (this.isExpanded()) {
      this.hasUnreadMessages.set(false);
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  sendMessage() {
    if (this.messageForm.invalid) return;
    const text = this.messageForm.value.message?.trim();
    if (!text) return;

    this.pushUser(text);
    this.messageForm.reset();
    this.send(text);
  }

  handleQuickAction(action: QuickAction) {
    this.pushUser(action.label);
    this.send(action.label);
  }

  private send(contenu: string) {
    this.isTyping.set(true);

    // On enrichit le payload avec les infos client/compte pour l'admin
    const payload: CreateChatPayload = {
      contenu,
      clientId:  this.getClientId(),
      clientNom: this.userName ?? this.getEmail(),
      numCompte: this.selectedAccount?.numCompte ?? null,
    };

    this.chatService.create(payload).subscribe({
      next: (res) => {
        this.isTyping.set(false);

        // Si demande sensible → message système l'indique
        if (res.statut === 'EN_ATTENTE') {
          this.pushBot(res.reponse ||
            '✅ Votre demande a été transmise à un conseiller. Vous recevrez une réponse rapidement.');
          return;
        }

        // Réponse normale (Ollama)
        this.pushBot(
          res.reponse
            ? res.reponse
            : `✅ Message enregistré (ticket #${res.id}). Un conseiller va vous répondre sous peu.`
        );
      },
      error: () => {
        this.isTyping.set(false);
        this.pushBot('❌ Service indisponible. Veuillez réessayer plus tard.');
      }
    });
  }

  private getClientId(): number | null {
    const idFromToken = (this.auth as any).clientIdFromToken;
    if (idFromToken && !isNaN(Number(idFromToken))) return Number(idFromToken);

    const user: any = this.auth.currentUser();
    const sub = user?.sub;
    if (sub) {
      const n = Number(sub);
      return isNaN(n) ? null : n;
    }
    return null;
  }

  private getEmail(): string | null {
    const user: any = this.auth.currentUser();
    return user?.email || user?.sub || null;
  }

  private pushUser(text: string) {
    this.messages.update(msgs => [...msgs, {
      id: this.generateId(), text, isBot: false, timestamp: new Date()
    }]);
    setTimeout(() => this.scrollToBottom(), 100);
  }

  private pushBot(text: string) {
    this.messages.update(msgs => [...msgs, {
      id: this.generateId(), text, isBot: true, timestamp: new Date()
    }]);
    if (!this.isExpanded()) this.hasUnreadMessages.set(true);
    setTimeout(() => this.scrollToBottom(), 100);
  }

  private generateId(): string {
    return Math.random().toString(36).substr(2, 9);
  }

  private scrollToBottom() {
    const el = document.querySelector('.chat-messages');
    if (el) el.scrollTop = el.scrollHeight;
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
