import {
  Component,
  signal,
  OnInit,
  OnDestroy,
  inject,
  Input
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject } from 'rxjs';

import { BankAccount } from '../../core/services/accounts.service';
import { Operation } from '../../core/services/operations.service';
import { ChatService, CreateChatPayload } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';

interface ChatUiMessage {
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

  private fb = inject(FormBuilder);
  private chatService = inject(ChatService);
  private auth = inject(AuthService);
  private destroy$ = new Subject<void>();

  private chatbotMessageListener = (event: any) => {
    const message = event.detail?.message;
    if (message) {
      this.isExpanded.set(true);
      this.hasUnreadMessages.set(false);
      this.messages.set([this.introMessage]);
      setTimeout(() => {
        this.pushUser(message);
        this.send(message);
      }, 300);
    }
  };

  isExpanded = signal(false);
  isTyping = signal(false);
  messages = signal<ChatUiMessage[]>([]);
  hasUnreadMessages = signal(false);

  quickActions = signal<QuickAction[]>([
    { label: 'Problème avec mon compte', icon: '🏦' },
    { label: 'Bloquer ma carte', icon: '🔒' },
    { label: 'Remplacer ma carte', icon: '💳' },
    { label: 'Problème de virement', icon: '💸' },
    { label: 'Taux épargne', icon: '📈' }
  ]);

  messageForm = this.fb.group({
    message: ['', [Validators.required, Validators.minLength(1)]]
  });

  private get introMessage(): ChatUiMessage {
    return {
      id: 'intro',
      text: `Bonjour ${this.userName ?? ''} 👋 Je suis <strong>Alex</strong>, votre conseiller T€chno-Bank. Comment puis-je vous aider ?`,
      isBot: true,
      timestamp: new Date()
    };
  }

  ngOnInit() {
    this.messages.set([this.introMessage]);
    window.addEventListener('open-chatbot-with-message', this.chatbotMessageListener);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    window.removeEventListener('open-chatbot-with-message', this.chatbotMessageListener);
  }

  toggleChat() {
    this.isExpanded.update(v => !v);

    if (this.isExpanded()) {
      this.hasUnreadMessages.set(false);
      this.messages.set([this.introMessage]);
      this.messageForm.reset();
      setTimeout(() => this.scrollToBottom(), 100);
    } else {
      this.messages.set([this.introMessage]);
      this.messageForm.reset();
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

    const payload: CreateChatPayload = {
      contenu,
      clientId: this.getClientId(),
      clientNom: this.userName ?? this.getEmail(),
      numCompte: this.selectedAccount?.numCompte ?? null
    };

    this.chatService.create(payload).subscribe({
      next: (res: any) => {
        this.isTyping.set(false);
        // ✅ Demande sensible → message générique, réponse admin attendue dans notifications
        if (res?.statut === 'EN_ATTENTE') {
          this.pushBot('✅ Votre demande a été transmise à un conseiller. Vous recevrez une réponse dans vos notifications.');
        } else {
          // ✅ Réponse Ollama normale
          this.pushBot(res?.reponse || '✅ Votre demande a été transmise.');
        }
      },
      error: () => {
        this.isTyping.set(false);
        this.pushBot('❌ Service indisponible. Veuillez réessayer plus tard.');
      }
    });
  }

  private getClientId(): number | null {
    const id = this.auth.clientIdFromToken;
    if (id != null && !isNaN(Number(id))) return Number(id);
    const user: any = this.auth.currentUser?.();
    if (user?.sub != null && !isNaN(Number(user.sub))) return Number(user.sub);
    return null;
  }

  private getEmail(): string | null {
    const user: any = this.auth.currentUser?.();
    return user?.email || null;
  }

  private pushUser(text: string) {
    this.messages.update(msgs => [
      ...msgs,
      { id: this.generateId(), text, isBot: false, timestamp: new Date() }
    ]);
    setTimeout(() => this.scrollToBottom(), 100);
  }

  private pushBot(text: string) {
    this.messages.update(msgs => [
      ...msgs,
      { id: this.generateId(), text, isBot: true, timestamp: new Date() }
    ]);
    if (!this.isExpanded()) this.hasUnreadMessages.set(true);
    setTimeout(() => this.scrollToBottom(), 100);
  }

  private generateId(): string {
    return Math.random().toString(36).slice(2, 11);
  }

  private scrollToBottom() {
    const el = document.querySelector('.chat-messages');
    if (el) el.scrollTop = el.scrollHeight;
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}