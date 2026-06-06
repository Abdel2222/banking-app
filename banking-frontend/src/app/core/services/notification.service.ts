import { Injectable, signal } from '@angular/core';

export interface Notification {
  id: number;
  type: 'rdv' | 'success' | 'info' | 'error' | 'chatbot';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  data?: any;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  notifications = signal<Notification[]>([]);
  private idCounter = 1;
  private notifiedChatIds = new Set<number>();

  addRDVNotification(date: string, time: string, type: string, agence: string, montant: number) {
    const notification: Notification = {
      id: this.idCounter++,
      type: 'rdv',
      title: `Rendez-vous ${type} confirmé`,
      message: `Votre RDV pour un ${type.toLowerCase()} de ${this.formatMontant(montant)} est prévu le ${date} à ${time} - Agence ${agence}`,
      timestamp: new Date(),
      read: false,
      data: { date, time, type, agence, montant }
    };

    this.notifications.set([notification, ...this.notifications()]);
  }

  addChatbotResponseNotification(chatId: number, reponseAdmin: string, dateReponse?: Date) {
    console.log('[NOTIF] addChatbotResponseNotification', { chatId, reponseAdmin, dateReponse });

    if (!chatId || !reponseAdmin?.trim()) return;

    if (this.notifiedChatIds.has(chatId)) {
      console.log('[NOTIF] déjà présent', chatId);
      return;
    }

    this.notifiedChatIds.add(chatId);

    const notification: Notification = {
      id: this.idCounter++,
      type: 'chatbot',
      title: '💬 Réponse du conseiller',
      message: reponseAdmin,
      timestamp: dateReponse || new Date(),
      read: false,
      data: { chatId }
    };

    this.notifications.set([notification, ...this.notifications()]);
    console.log('[NOTIF] état courant =', this.notifications());
  }

  markAsRead(id: number) {
    this.notifications.set(
      this.notifications().map(n =>
        n.id === id ? { ...n, read: true } : n
      )
    );
  }

  getUnreadCount(): number {
    return this.notifications().filter(n => !n.read).length;
  }

  clearAll() {
    this.notifications.set([]);
    this.notifiedChatIds.clear();
  }

  deleteNotification(id: number) {
    const notif = this.notifications().find(n => n.id === id);

    if (notif?.type === 'chatbot' && notif.data?.chatId) {
      this.notifiedChatIds.delete(notif.data.chatId);
    }

    this.notifications.set(
      this.notifications().filter(n => n.id !== id)
    );
  }

  private formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(montant);
  }
}
