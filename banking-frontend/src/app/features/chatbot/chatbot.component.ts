import { Component, signal, OnInit, OnDestroy, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { BankAccount } from '../../core/services/accounts.service';
import { Operation } from '../../core/services/operations.service'; // Utiliser l'interface du service

interface ChatMessage {
  id: string;
  text: string;
  isBot: boolean;
  timestamp: Date;
  type?: 'text' | 'action' | 'info';
}

interface QuickAction {
  label: string;
  action: string;
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
  private destroy$ = new Subject<void>();

  // State
  isExpanded = signal<boolean>(false);
  isTyping = signal<boolean>(false);
  messages = signal<ChatMessage[]>([]);
  quickActions = signal<QuickAction[]>([]);
  hasUnreadMessages = signal<boolean>(false);

  // Form
  messageForm = this.fb.group({
    message: ['', [Validators.required, Validators.minLength(1)]]
  });

  private botResponses = {
    greeting: [
      "Bonjour ! Je suis votre assistant bancaire virtuel. Comment puis-je vous aider aujourd'hui ?",
      "Salut ! Que souhaitez-vous savoir sur vos comptes ou nos services ?",
      "Hello ! Je suis là pour répondre à vos questions bancaires."
    ],
    balance: [
      "Votre solde actuel est de {balance}. Souhaitez-vous effectuer une opération ?",
      "Compte {account} : {balance} disponibles.",
      "Solde disponible : {balance}"
    ],
    help: [
      "Je peux vous aider avec :<br/>• Consulter votre solde<br/>• Historique des opérations<br/>• Gestion de carte<br/>• Informations sur les services<br/>• Assistance générale",
      "Voici ce que je peux faire pour vous :<br/>📊 Soldes et comptes<br/>💳 Gestion de carte<br/>📋 Historique<br/>❓ Questions diverses"
    ],
    operations: [
      "Voici vos {count} dernières opérations :",
      "Historique récent de votre compte :"
    ],
    card: [
      "Votre carte se termine par {lastDigits} et expire le {expiry}. Statut : {status}",
      "Informations carte : **** **** **** {lastDigits} - Expire : {expiry} - {status}"
    ],
    unknown: [
      "Je ne suis pas sûr de comprendre. Pourriez-vous reformuler votre question ?",
      "Désolé, je n'ai pas saisi. Pouvez-vous être plus précis ?",
      "Je ne comprends pas très bien. Essayez 'aide' pour voir ce que je peux faire."
    ],
    goodbye: [
      "Au revoir ! N'hésitez pas à revenir si vous avez des questions.",
      "À bientôt ! Je reste disponible pour vous aider.",
      "Bonne journée ! Revenez quand vous voulez."
    ]
  };

  ngOnInit() {
    this.initializeChat();
    this.updateQuickActions();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeChat() {
    const welcomeMessage: ChatMessage = {
      id: this.generateId(),
      text: this.getRandomResponse('greeting'),
      isBot: true,
      timestamp: new Date(),
      type: 'text'
    };
    this.messages.set([welcomeMessage]);
  }

  private updateQuickActions() {
    const actions: QuickAction[] = [
      { label: 'Mon solde', action: 'balance', icon: '💰' },
      { label: 'Mes opérations', action: 'operations', icon: '📊' },
      { label: 'Ma carte', action: 'card', icon: '💳' },
      { label: 'Aide', action: 'help', icon: '❓' }
    ];
    this.quickActions.set(actions);
  }

  toggleChat() {
    const expanded = !this.isExpanded();
    this.isExpanded.set(expanded);

    if (expanded) {
      this.hasUnreadMessages.set(false);
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  sendMessage() {
    if (this.messageForm.invalid) return;

    const messageText = this.messageForm.value.message?.trim();
    if (!messageText) return;

    // Add user message
    const userMessage: ChatMessage = {
      id: this.generateId(),
      text: messageText,
      isBot: false,
      timestamp: new Date(),
      type: 'text'
    };

    this.messages.update(msgs => [...msgs, userMessage]);
    this.messageForm.reset();

    // Process message
    setTimeout(() => {
      this.processUserMessage(messageText);
    }, 500);

    setTimeout(() => this.scrollToBottom(), 100);
  }

  handleQuickAction(action: QuickAction) {
    const userMessage: ChatMessage = {
      id: this.generateId(),
      text: action.label,
      isBot: false,
      timestamp: new Date(),
      type: 'action'
    };

    this.messages.update(msgs => [...msgs, userMessage]);

    setTimeout(() => {
      this.processUserMessage(action.action);
    }, 300);

    setTimeout(() => this.scrollToBottom(), 100);
  }

  private processUserMessage(text: string) {
    this.isTyping.set(true);

    setTimeout(() => {
      const response = this.generateBotResponse(text.toLowerCase());
      const botMessage: ChatMessage = {
        id: this.generateId(),
        text: response,
        isBot: true,
        timestamp: new Date(),
        type: 'text'
      };

      this.messages.update(msgs => [...msgs, botMessage]);
      this.isTyping.set(false);

      if (!this.isExpanded()) {
        this.hasUnreadMessages.set(true);
      }

      setTimeout(() => this.scrollToBottom(), 100);
    }, 1000 + Math.random() * 1000);
  }

  private generateBotResponse(input: string): string {
    // Keywords detection
    if (this.containsKeywords(input, ['solde', 'balance', 'argent', 'compte'])) {
      const balance = this.selectedAccount?.balance || 0;
      const account = this.selectedAccount?.numCompte || '';
      return this.getRandomResponse('balance')
        .replace('{balance}', this.formatCurrency(balance))
        .replace('{account}', account);
    }

    if (this.containsKeywords(input, ['opération', 'historique', 'transaction', 'mouvement'])) {
      const count = this.operations?.length || 0;
      let response = this.getRandomResponse('operations').replace('{count}', count.toString());

      if (count > 0) {
        const opsText = this.operations.slice(0, 3).map(op =>
          `• ${this.formatDate(op.date)} : ${op.description} (${this.formatCurrency(op.montant)})`
        ).join('<br/>');
        response += '<br/><br/>' + opsText;
        if (count > 3) {
          response += `<br/><em>... et ${count - 3} autres opérations</em>`;
        }
      } else {
        response += '<br/><br/>Aucune opération récente trouvée.';
      }

      return response;
    }

    if (this.containsKeywords(input, ['carte', 'cb', 'visa', 'mastercard'])) {
      // Simulated card info (you'd get this from your cards service)
      return this.getRandomResponse('card')
        .replace('{lastDigits}', '1234')
        .replace('{expiry}', '12/25')
        .replace('{status}', 'Active');
    }

    if (this.containsKeywords(input, ['aide', 'help', 'que', 'comment', 'faire'])) {
      return this.getRandomResponse('help');
    }

    if (this.containsKeywords(input, ['merci', 'au revoir', 'bye', 'salut', 'tchao'])) {
      return this.getRandomResponse('goodbye');
    }

    if (this.containsKeywords(input, ['bonjour', 'hello', 'salut', 'coucou'])) {
      const greeting = this.getRandomResponse('greeting');
      const userName = this.userName ? ` ${this.userName.split(' ')[0]}` : '';
      return greeting.replace('Bonjour !', `Bonjour${userName} !`);
    }

    return this.getRandomResponse('unknown');
  }

  private containsKeywords(text: string, keywords: string[]): boolean {
    return keywords.some(keyword => text.includes(keyword));
  }

  private getRandomResponse(category: keyof typeof this.botResponses): string {
    const responses = this.botResponses[category];
    return responses[Math.floor(Math.random() * responses.length)];
  }

  private generateId(): string {
    return Math.random().toString(36).substr(2, 9);
  }

  private scrollToBottom() {
    const container = document.querySelector('.chat-messages');
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  private formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  private formatDate(date: string | null | undefined): string {
    if (!date) return 'N/A';
    try {
      return new Date(date).toLocaleDateString('fr-FR');
    } catch {
      return 'Date invalide';
    }
  }
}
