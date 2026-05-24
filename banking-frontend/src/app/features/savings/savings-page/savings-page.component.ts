import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../../core/services/auth.service';
import { InteretService, Interet } from '../../../core/services/interet.service';
import { SavingsService, CompteEpargne } from '../../../core/services/savings.service';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';

@Component({
  selector: 'app-savings-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MoneyPipe, DatePipe, DecimalPipe],
  templateUrl: './savings-page.component.html',
  styleUrls: ['./savings-page.component.scss']
})
export class SavingsPageComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private interetSvc = inject(InteretService);
  private savingsSvc = inject(SavingsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private destroy$ = new Subject<void>();

  // ===== Signals d'état =====
  loading = signal<boolean>(true);
  busy = signal<boolean>(false);
  error = signal<string | null>(null);

  numCompte = signal<string | null>(null);
  compte = signal<CompteEpargne | null>(null);
  interetCourant = signal<Interet | null>(null);
  historique = signal<Interet[]>([]);
  interetsEnCours = signal<number>(0);

  // Période sélectionnée pour le graphique
  periode = signal<'6M' | '1A' | 'ALL'>('6M');

  // Simulateur
  anneesSimulation = signal<number>(5);

  // Modals
  showAlimenterModal = signal<boolean>(false);
  showRetirerModal = signal<boolean>(false);
  montantInput = signal<number>(100);

  // ===== Computed =====
  taux = computed(() => {
    const i = this.interetCourant();
    return i ? Number(i.tauxInteret) : 0;
  });

  totalInteretsCumules = computed(() => {
    return this.historique()
      .filter(i => i.dateCapitalisation)
      .reduce((sum, i) => sum + Number(i.montantInteret || 0), 0);
  });

  prochaineCapitalisation = computed(() => {
    const i = this.interetCourant();
    if (!i) return null;
    const base = i.dateCapitalisation
      ? new Date(i.dateCapitalisation)
      : new Date(i.dateDebut);
    const next = new Date(base);
    next.setMonth(next.getMonth() + 1);
    return next;
  });

  joursAvantProchaineCapi = computed(() => {
    const next = this.prochaineCapitalisation();
    if (!next) return 0;
    const diff = next.getTime() - new Date().getTime();
    return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
  });

  /** Projection: solde dans X années avec intérêts composés */
  projection = computed(() => {
    const solde = this.compte()?.solde || 0;
    const taux = this.taux();
    const annees = this.anneesSimulation();
    if (!solde || !taux || !annees) return solde;
    return solde * Math.pow(1 + taux, annees);
  });

  gainProjection = computed(() => {
    return this.projection() - (this.compte()?.solde || 0);
  });

  /** Construit les points du graphique d'évolution */
  graphData = computed(() => {
    const c = this.compte();
    const histo = this.historique();
    if (!c) return { points: [], capitalisations: [], minY: 0, maxY: 100 };

    const soldeActuel = c.solde || 0;

    // On reconstruit l'historique du solde en partant du solde actuel et en soustrayant les intérêts capitalisés
    const points: { date: Date; solde: number }[] = [];

    // Date de création du compte
    const dateCreation = new Date(c.createdAt);

    // Tri historique par date croissante
    const capitalisations = histo
      .filter(i => i.dateCapitalisation && i.montantInteret > 0)
      .sort((a, b) =>
        new Date(a.dateCapitalisation!).getTime() - new Date(b.dateCapitalisation!).getTime()
      );

    // Point initial (ouverture du compte = premier montant)
    points.push({
      date: dateCreation,
      solde: Number(c.premierMontant) || 0
    });

    // Soldes après chaque capitalisation
    let cumul = Number(c.premierMontant) || 0;
    for (const cap of capitalisations) {
      cumul += Number(cap.montantInteret);
      points.push({
        date: new Date(cap.dateCapitalisation!),
        solde: cumul
      });
    }

    // Point final = aujourd'hui avec solde actuel
    points.push({
      date: new Date(),
      solde: soldeActuel
    });

    const soldes = points.map(p => p.solde);
    const minY = Math.min(...soldes) * 0.95;
    const maxY = Math.max(...soldes) * 1.05;

    return {
      points,
      capitalisations: capitalisations.map(c => ({
        date: new Date(c.dateCapitalisation!),
        montant: Number(c.montantInteret)
      })),
      minY,
      maxY
    };
  });

  /** Path SVG pour la courbe */
  svgPath = computed(() => {
    const data = this.graphData();
    if (!data.points.length) return '';

    const W = 600;
    const H = 160;
    const minDate = data.points[0].date.getTime();
    const maxDate = data.points[data.points.length - 1].date.getTime();
    const rangeDate = Math.max(1, maxDate - minDate);
    const rangeY = Math.max(0.01, data.maxY - data.minY);

    const coords = data.points.map(p => {
      const x = ((p.date.getTime() - minDate) / rangeDate) * W;
      const y = H - ((p.solde - data.minY) / rangeY) * H;
      return { x, y };
    });

    let path = `M ${coords[0].x} ${coords[0].y}`;
    for (let i = 1; i < coords.length; i++) {
      path += ` L ${coords[i].x} ${coords[i].y}`;
    }
    return path;
  });

  /** Path pour la zone sous la courbe */
  svgArea = computed(() => {
    const path = this.svgPath();
    if (!path) return '';
    return `${path} L 600 160 L 0 160 Z`;
  });

  /** Coordonnées des points de capitalisation pour le SVG */
  capiPoints = computed(() => {
    const data = this.graphData();
    if (!data.points.length) return [];

    const W = 600;
    const H = 160;
    const minDate = data.points[0].date.getTime();
    const maxDate = data.points[data.points.length - 1].date.getTime();
    const rangeDate = Math.max(1, maxDate - minDate);
    const rangeY = Math.max(0.01, data.maxY - data.minY);

    return data.capitalisations.map(cap => {
      // Trouve le solde à cette date
      const pointMatch = data.points.find(p =>
        Math.abs(p.date.getTime() - cap.date.getTime()) < 24 * 60 * 60 * 1000
      );
      const solde = pointMatch?.solde || 0;
      return {
        x: ((cap.date.getTime() - minDate) / rangeDate) * W,
        y: H - ((solde - data.minY) / rangeY) * H,
        montant: cap.montant,
        date: cap.date
      };
    });
  });

  isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  ngOnInit() {
    const numFromUrl = this.route.snapshot.queryParamMap.get('numCompte');
    if (!numFromUrl) {
      this.error.set('Aucun compte spécifié');
      this.loading.set(false);
      return;
    }
    this.numCompte.set(numFromUrl);
    this.loadAll();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAll() {
    const num = this.numCompte();
    if (!num) return;

    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      compte: this.savingsSvc.getDetails(num).pipe(
        catchError(e => { console.error('getDetails KO', e); return of(null); })
      ),
      historique: this.interetSvc.historique(num).pipe(
        catchError(e => { console.error('historique KO', e); return of([]); })
      ),
      calcul: this.interetSvc.calculer(num).pipe(
        catchError(() => of({ numCompte: num, montantInterets: 0 }))
      )
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ compte, historique, calcul }) => {
        this.compte.set(compte);
        this.historique.set(historique || []);
        this.interetsEnCours.set(calcul?.montantInterets || 0);

        // Le dernier intérêt = celui en cours (ou le plus récent)
        const sorted = [...(historique || [])].sort((a, b) =>
          new Date(b.dateDebut).getTime() - new Date(a.dateDebut).getTime()
        );
        this.interetCourant.set(sorted[0] || null);

        this.loading.set(false);
      },
      error: (e) => {
        this.error.set('Erreur de chargement: ' + (e?.message || ''));
        this.loading.set(false);
      }
    });
  }

  setPeriode(p: '6M' | '1A' | 'ALL') {
    this.periode.set(p);
  }

  // ===== Actions =====

  openAlimenter() {
    this.montantInput.set(100);
    this.showAlimenterModal.set(true);
  }

  openRetirer() {
    this.montantInput.set(50);
    this.showRetirerModal.set(true);
  }

  closeModals() {
    this.showAlimenterModal.set(false);
    this.showRetirerModal.set(false);
  }

  alimenter() {
    const num = this.numCompte();
    const montant = this.montantInput();
    if (!num || !montant || montant <= 0) return;

    this.busy.set(true);
    this.savingsSvc.alimenter(num, montant).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        this.closeModals();
        this.loadAll();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur alimentation: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  retirer() {
    const num = this.numCompte();
    const montant = this.montantInput();
    if (!num || !montant || montant <= 0) return;

    this.busy.set(true);
    this.savingsSvc.retirer(num, montant).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        this.closeModals();
        this.loadAll();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur retrait: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  capitaliser() {
    const num = this.numCompte();
    if (!num) return;

    if (!confirm('⚡ Capitaliser les intérêts maintenant ?\n\nLe montant sera crédité sur le solde du compte épargne.')) return;

    this.busy.set(true);
    this.interetSvc.capitaliser(num).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        alert('✨ Intérêts capitalisés avec succès !');
        this.loadAll();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur capitalisation: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  retour() {
    this.router.navigate(['/dashboard']);
  }
}
