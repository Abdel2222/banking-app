import { Injectable, signal } from '@angular/core';

export interface Notification {
  id: number;
  type: 'rdv' | 'success' | 'info' | 'error';
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

  addRDVNotification(date: string, time: string, type: string, agence: string, montant: number) {
    const notification: Notification = {
      id: this.idCounter++,
      type: 'rdv',
      title: `Rendez-vous ${type} confirmé`,
      message: `Votre RDV pour un ${type.toLowerCase()} de ${this.formatMontant(montant)} est prévu le ${date} à ${time} - Agence ${agence}`,
      timestamp: new Date(),
      read: false,
      data: {
        date,
        time,
        type,
        agence,
        montant
      }
    };

    const current = this.notifications();
    this.notifications.set([notification, ...current]);

    console.log('[NOTIFICATION SERVICE] Notification ajoutée:', notification);
  }

  markAsRead(id: number) {
    const updated = this.notifications().map(n =>
      n.id === id ? { ...n, read: true } : n
    );
    this.notifications.set(updated);
  }

  getUnreadCount(): number {
    return this.notifications().filter(n => !n.read).length;
  }

  clearAll() {
    this.notifications.set([]);
  }

  private formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(montant);
  }
}
