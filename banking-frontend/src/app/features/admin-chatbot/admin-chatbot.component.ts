import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Subject, takeUntil, interval } from 'rxjs';
import { HttpClient } from '@angular/common/http';

interface ChatInteraction {
  id: string;
  userId: string;
  userName: string;
  timestamp: Date;
  userMessage: string;
  botResponse: string;
  context: {
    accountNumber?: string;
    balance?: number;
    action?: string;
  };
  sentiment?: 'positive' | 'neutral' | 'negative';
  category: 'balance' | 'operations' | 'card' | 'help' | 'other';
  resolved: boolean;
  responseTime?: number; // en secondes
}

interface ChatStats {
  totalInteractions: number;
  uniqueUsers: number;
  topQuestions: Array<{ question: string; count: number }>;
  resolutionRate: number;
  avgResponseTime: number;
  todayInteractions: number;
  satisfactionRate: number;
}

@Component({
  selector: 'app-admin-chatbot',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-chatbot.component.html',
  styleUrls: ['./admin-chatbot.component.scss']
})
export class AdminChatbotComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private destroy$ = new Subject<void>();

  // State
  interactions = signal<ChatInteraction[]>([]);
  filteredInteractions = signal<ChatInteraction[]>([]);
  stats = signal<ChatStats>({
    totalInteractions: 0,
    uniqueUsers: 0,
    topQuestions: [],
    resolutionRate: 0,
    avgResponseTime: 0,
    todayInteractions: 0,
    satisfactionRate: 0
  });

  // Modal state
  showUserHistory = signal<boolean>(false);
  selectedUser = signal<string>('');
  userHistory = signal<ChatInteraction[]>([]);

  // Loading state
  loading = signal<boolean>(false);

  // Filters
  categoryFilter = '';
  sentimentFilter = '';
  dateFilter = '';

  ngOnInit() {
    this.loadInteractions();

    // Auto-refresh every 30 seconds
    interval(30000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadInteractions();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadInteractions() {
    this.loading.set(true);

    // Simulate API call - replace with real endpoint
    this.http.get<ChatInteraction[]>('/api/admin/chatbot/interactions')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.interactions.set(data);
          this.applyFilters();
          this.calculateStats();
          this.loading.set(false);
        },
        error: (error) => {
          console.error('Error loading interactions:', error);
          // Fallback with enhanced mock data
          this.loadEnhancedMockData();
          this.loading.set(false);
        }
      });
  }

  private loadEnhancedMockData() {
    // Enhanced mock data with more realistic scenarios
    const mockInteractions: ChatInteraction[] = [
      {
        id: '1',
        userId: 'user123',
        userName: 'Jean Dupont',
        timestamp: new Date(),
        userMessage: 'Quel est mon solde actuel ?',
        botResponse: 'Votre solde actuel est de 1,250.50 €. Souhaitez-vous effectuer une opération ?',
        context: { accountNumber: '12345678', balance: 1250.50, action: 'balance_check' },
        sentiment: 'neutral',
        category: 'balance',
        resolved: true,
        responseTime: 1.2
      },
      {
        id: '2',
        userId: 'user456',
        userName: 'Marie Martin',
        timestamp: new Date(Date.now() - 3600000),
        userMessage: 'Ma carte bancaire ne fonctionne pas au distributeur',
        botResponse: 'Je vois que votre carte est temporairement bloquée pour des raisons de sécurité. Je vous aide à la débloquer immédiatement.',
        context: { accountNumber: '87654321', action: 'card_issue' },
        sentiment: 'negative',
        category: 'card',
        resolved: false,
        responseTime: 2.1
      },
      {
        id: '3',
        userId: 'user789',
        userName: 'Pierre Durand',
        timestamp: new Date(Date.now() - 7200000),
        userMessage: 'Comment faire un virement vers un autre compte ?',
        botResponse: 'Voici la procédure pour effectuer un virement :<br/>1. Allez dans "Virements"<br/>2. Sélectionnez le compte bénéficiaire<br/>3. Saisissez le montant<br/>4. Confirmez avec votre code',
        context: { accountNumber: '11223344', action: 'transfer_help' },
        sentiment: 'positive',
        category: 'help',
        resolved: true,
        responseTime: 0.8
      },
      {
        id: '4',
        userId: 'user101',
        userName: 'Sophie Lemaire',
        timestamp: new Date(Date.now() - 10800000),
        userMessage: 'Montrez-moi mes dernières opérations',
        botResponse: 'Voici vos 5 dernières opérations :<br/>• 25/09/2025 : Achat CB Carrefour (-45.60 €)<br/>• 24/09/2025 : Virement reçu (+500.00 €)<br/>• 23/09/2025 : Prélèvement EDF (-67.80 €)',
        context: { accountNumber: '55667788', balance: 980.30, action: 'operations_list' },
        sentiment: 'neutral',
        category: 'operations',
        resolved: true,
        responseTime: 1.5
      },
      {
        id: '5',
        userId: 'user202',
        userName: 'Lucas Bernard',
        timestamp: new Date(Date.now() - 14400000),
        userMessage: 'Je n\'arrive pas à me connecter à mon compte',
        botResponse: 'Je comprends votre problème de connexion. Avez-vous essayé de réinitialiser votre mot de passe ? Je peux vous guider dans cette démarche.',
        context: { accountNumber: '99887766', action: 'login_issue' },
        sentiment: 'negative',
        category: 'help',
        resolved: false,
        responseTime: 1.8
      },
      {
        id: '6',
        userId: 'user303',
        userName: 'Emma Dubois',
        timestamp: new Date(Date.now() - 18000000),
        userMessage: 'Quels sont les frais pour un virement international ?',
        botResponse: 'Les frais pour un virement international sont :<br/>• Zone SEPA : 0.50 €<br/>• Hors SEPA : 15 € + 0.1% du montant<br/>• Frais de change appliqués selon le taux du jour',
        context: { action: 'fees_inquiry' },
        sentiment: 'neutral',
        category: 'help',
        resolved: true,
        responseTime: 2.3
      },
      {
        id: '7',
        userId: 'user404',
        userName: 'Thomas Roux',
        timestamp: new Date(Date.now() - 21600000),
        userMessage: 'Ma carte a été avalée par le distributeur',
        botResponse: 'C\'est embêtant ! Je bloque immédiatement votre carte pour éviter tout usage frauduleux. Une nouvelle carte vous sera envoyée sous 3-5 jours ouvrés.',
        context: { accountNumber: '33445566', action: 'card_blocked' },
        sentiment: 'negative',
        category: 'card',
        resolved: true,
        responseTime: 1.0
      },
      {
        id: '8',
        userId: 'user123',
        userName: 'Jean Dupont',
        timestamp: new Date(Date.now() - 25200000),
        userMessage: 'Merci pour votre aide, tout fonctionne parfaitement !',
        botResponse: 'Je suis ravi d\'avoir pu vous aider ! N\'hésitez pas à me recontacter si vous avez d\'autres questions.',
        context: { accountNumber: '12345678', action: 'satisfaction_positive' },
        sentiment: 'positive',
        category: 'other',
        resolved: true,
        responseTime: 0.5
      }
    ];

    this.interactions.set(mockInteractions);
    this.applyFilters();
    this.calculateStats();
  }

  private calculateStats() {
    const interactions = this.interactions();
    const totalInteractions = interactions.length;
    const uniqueUsers = new Set(interactions.map(i => i.userId)).size;
    const resolved = interactions.filter(i => i.resolved).length;
    const resolutionRate = totalInteractions > 0 ? Math.round((resolved / totalInteractions) * 100) : 0;

    // Calculate today's interactions
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayInteractions = interactions.filter(i => i.timestamp >= today).length;

    // Calculate average response time
    const responseTimes = interactions.filter(i => i.responseTime).map(i => i.responseTime!);
    const avgResponseTime = responseTimes.length > 0
      ? Math.round((responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length) * 10) / 10
      : 0;

    // Calculate satisfaction rate (based on positive sentiment)
    const positiveInteractions = interactions.filter(i => i.sentiment === 'positive').length;
    const satisfactionRate = totalInteractions > 0 ? Math.round((positiveInteractions / totalInteractions) * 100) : 0;

    // Count questions
    const questionCounts = new Map<string, number>();
    interactions.forEach(i => {
      const question = i.userMessage.toLowerCase();
      const key = this.normalizeQuestion(question);
      questionCounts.set(key, (questionCounts.get(key) || 0) + 1);
    });

    const topQuestions = Array.from(questionCounts.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([question, count]) => ({ question, count }));

    this.stats.set({
      totalInteractions,
      uniqueUsers,
      topQuestions,
      resolutionRate,
      avgResponseTime,
      todayInteractions,
      satisfactionRate
    });
  }

  private normalizeQuestion(question: string): string {
    // Normalize similar questions
    if (question.includes('solde') || question.includes('balance')) return 'Consultation solde';
    if (question.includes('carte') || question.includes('cb')) return 'Problème carte';
    if (question.includes('virement') || question.includes('transfer')) return 'Aide virement';
    if (question.includes('opération') || question.includes('transaction')) return 'Historique opérations';
    if (question.includes('connexion') || question.includes('login')) return 'Problème connexion';
    if (question.includes('frais') || question.includes('tarif')) return 'Information tarifs';
    return question.substring(0, 30) + '...';
  }

  private applyFilters() {
    let filtered = this.interactions();

    if (this.categoryFilter) {
      filtered = filtered.filter(i => i.category === this.categoryFilter);
    }

    if (this.sentimentFilter) {
      filtered = filtered.filter(i => i.sentiment === this.sentimentFilter);
    }

    if (this.dateFilter) {
      const filterDate = new Date();
      switch (this.dateFilter) {
        case 'today':
          filterDate.setHours(0, 0, 0, 0);
          filtered = filtered.filter(i => i.timestamp >= filterDate);
          break;
        case 'week':
          filterDate.setDate(filterDate.getDate() - 7);
          filtered = filtered.filter(i => i.timestamp >= filterDate);
          break;
        case 'month':
          filterDate.setMonth(filterDate.getMonth() - 1);
          filtered = filtered.filter(i => i.timestamp >= filterDate);
          break;
      }
    }

    // Sort by timestamp (most recent first)
    filtered.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());

    this.filteredInteractions.set(filtered);
  }

  filterByCategory(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.categoryFilter = target.value;
    this.applyFilters();
  }

  filterBySentiment(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.sentimentFilter = target.value;
    this.applyFilters();
  }

  filterByDate(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.dateFilter = target.value;
    this.applyFilters();
  }

  markAsResolved(interaction: ChatInteraction) {
    // Update locally
    const interactions = this.interactions();
    const index = interactions.findIndex(i => i.id === interaction.id);
    if (index !== -1) {
      interactions[index].resolved = true;
      this.interactions.set([...interactions]);
      this.applyFilters();
      this.calculateStats();
    }

    // Send to API
    this.http.patch(`/api/admin/chatbot/interactions/${interaction.id}`, { resolved: true })
      .subscribe({
        next: () => console.log('Marked as resolved'),
        error: (error) => console.error('Error updating interaction:', error)
      });
  }

  flagForReview(interaction: ChatInteraction) {
    // Add visual feedback
    const interactions = this.interactions();
    const index = interactions.findIndex(i => i.id === interaction.id);
    if (index !== -1) {
      // You could add a 'flagged' property to the interface
      console.log('Flagged interaction:', interaction.id);
    }

    this.http.post(`/api/admin/chatbot/interactions/${interaction.id}/flag`, {})
      .subscribe({
        next: () => console.log('Flagged for review'),
        error: (error) => console.error('Error flagging interaction:', error)
      });
  }

  viewUserHistory(userId: string) {
    const userInteractions = this.interactions().filter(i => i.userId === userId);
    const userName = userInteractions[0]?.userName || userId;
    this.selectedUser.set(userName);
    this.userHistory.set(userInteractions);
    this.showUserHistory.set(true);
  }

  closeModal() {
    this.showUserHistory.set(false);
  }

  refreshData() {
    this.loadInteractions();
  }

  exportData() {
    const data = this.filteredInteractions();
    const csv = this.convertToCSV(data);
    this.downloadCSV(csv, `chatbot-interactions-${new Date().toISOString().split('T')[0]}.csv`);
  }

  private convertToCSV(data: ChatInteraction[]): string {
    const headers = ['Date', 'Utilisateur', 'Message', 'Réponse', 'Catégorie', 'Sentiment', 'Résolu', 'Temps réponse'];
    const rows = data.map(i => [
      this.formatTime(i.timestamp),
      i.userName,
      i.userMessage.replace(/"/g, '""'),
      i.botResponse.replace(/<br\/>/g, ' ').replace(/"/g, '""'),
      this.getCategoryLabel(i.category),
      i.sentiment || '',
      i.resolved ? 'Oui' : 'Non',
      i.responseTime ? `${i.responseTime}s` : ''
    ]);

    return [headers, ...rows]
      .map(row => row.map(field => `"${field}"`).join(','))
      .join('\n');
  }

  private downloadCSV(csv: string, filename: string) {
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  formatTime(date: Date): string {
    return date.toLocaleString('fr-FR');
  }

  formatTimeShort(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `${diffMins}min`;
    if (diffHours < 24) return `${diffHours}h`;
    if (diffDays < 7) return `${diffDays}j`;
    return date.toLocaleDateString('fr-FR');
  }

  getCategoryLabel(category: string): string {
    const labels: Record<string, string> = {
      'balance': 'Solde',
      'operations': 'Opérations',
      'card': 'Carte',
      'help': 'Aide',
      'other': 'Autre'
    };
    return labels[category] || category;
  }

  getSentimentIcon(sentiment?: string): string {
    const icons: Record<string, string> = {
      'positive': '😊',
      'neutral': '😐',
      'negative': '😞'
    };
    return icons[sentiment || 'neutral'] || '😐';
  }

  getSentimentClass(sentiment?: string): string {
    return `sentiment-${sentiment || 'neutral'}`;
  }

  getResponseTimeClass(responseTime?: number): string {
    if (!responseTime) return '';
    if (responseTime <= 1) return 'fast';
    if (responseTime <= 2) return 'normal';
    return 'slow';
  }
}
