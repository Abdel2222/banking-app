import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CardsService, CardInfo } from '../../core/services/cards.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-card-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-details.component.html',
  styleUrls: ['./card-details.component.scss']
})
export class CardDetailsComponent implements OnInit {
  private cardsService = inject(CardsService);
  private authService  = inject(AuthService);
  private router       = inject(Router);
  private route        = inject(ActivatedRoute);
  private http         = inject(HttpClient);

  private readonly API_BASE = 'http://localhost:8084/api';

  card      = signal<CardInfo | null>(null);
  loading   = signal(true);
  error     = signal<string | null>(null);
  numCompte = signal<string>('');
  userName  = signal<string>('');
  isFlipped = signal(false);

  // ✅ CVV réel récupéré depuis le backend via AES
  cvv = signal<string>('•••');

  ngOnInit() {
    const numCompte = this.route.snapshot.queryParams['numCompte'];

    if (!numCompte) {
      this.error.set('Numéro de compte manquant');
      this.loading.set(false);
      return;
    }

    this.numCompte.set(numCompte);

    const user = this.authService.currentUser();
    this.userName.set(user?.nomComplet || user?.email || 'CLIENT');

    this.loadCardDetails(numCompte);
  }

  private loadCardDetails(numCompte: string) {
    this.loading.set(true);

    this.cardsService.getByAccount(numCompte).subscribe({
      next: (cardData) => {
        this.card.set(cardData);
        this.loading.set(false);
        // ✅ Charger le vrai CVV depuis le backend
        this.loadCvv(numCompte);
      },
      error: (err) => {
        console.error('Erreur chargement carte:', err);
        this.error.set('Impossible de charger les informations de la carte');
        this.loading.set(false);
      }
    });
  }

  // ✅ Appelle GET /api/cartes/{numCompte}/mon-cvv pour déchiffrer AES
  private loadCvv(numCompte: string) {
    this.http.get<any>(`${this.API_BASE}/cartes/${numCompte}/mon-cvv`).subscribe({
      next: (resp) => {
        const cvv = resp?.cvv ?? '•••';
        this.cvv.set(cvv);
      },
      error: () => this.cvv.set('•••')
    });
  }

  toggleFlip() {
    this.isFlipped.set(!this.isFlipped());
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  formatCardNumber(cardNumber: string | undefined): string {
    if (!cardNumber) return '•••• •••• •••• ••••';
    const clean = cardNumber.replace(/\s/g, '');
    return clean.match(/.{1,4}/g)?.join(' ') || cardNumber;
  }

  formatExpirationDate(date: string | undefined): string {
    if (!date) return 'MM/AA';
    try {
      const dateObj = new Date(date);
      const month = String(dateObj.getMonth() + 1).padStart(2, '0');
      const year  = String(dateObj.getFullYear()).slice(-2);
      return `${month}/${year}`;
    } catch {
      return 'MM/AA';
    }
  }

  // ✅ Retourne le vrai CVV depuis le signal
  getCVV(): string {
    return this.cvv();
  }

  get cardStatus(): string {
    return this.card()?.estActive ? 'Active' : 'Bloquée';
  }

  get cardStatusClass(): string {
    return this.card()?.estActive ? 'active' : 'blocked';
  }
}
