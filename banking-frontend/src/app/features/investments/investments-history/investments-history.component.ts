import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  InvestmentService,
  Placement,
  StatutPlacement
} from '../investment.service';
import { OperationsStoreService } from '../../../core/services/operations-store.service';

@Component({
  selector: 'app-investments-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './investments-history.component.html',
  styleUrl: './investments-history.component.scss'
})
export class InvestmentsHistoryComponent implements OnInit {
  private readonly investmentService = inject(InvestmentService);
  private readonly operationsStore   = inject(OperationsStoreService);
  private readonly router            = inject(Router);

  placements   = signal<Placement[]>([]);
  search       = signal<string>('');
  statusFilter = signal<'TOUS' | StatutPlacement>('TOUS');
  loading      = signal<boolean>(false);
  closingId    = signal<number | null>(null);
  sortingId    = signal<number | null>(null);
  error        = signal<string>('');
  success      = signal<string>('');

  /* ==================== Computed ==================== */

  filteredPlacements = computed(() => {
    const query  = this.search().trim().toLowerCase();
    const statut = this.statusFilter();
    return this.placements().filter((p) => {
      const matchSearch =
        !query ||
        p.nomFonds?.toLowerCase().includes(query) ||
        p.numeroCompte?.toLowerCase().includes(query) ||
        p.clientNomComplet?.toLowerCase().includes(query);
      return matchSearch && (statut === 'TOUS' || p.statut === statut);
    });
  });

  totalCapitalInvesti = computed(() =>
    this.placements()
      .filter(p => p.statut === 'ACTIF')
      .reduce((t, p) => t + Number(p.montant ?? 0), 0)
  );

  totalGainPrevu = computed(() =>
    this.placements()
      .filter(p => p.statut === 'ACTIF')
      .reduce((t, p) => t + Number(p.gainPrevu ?? 0), 0)
  );

  activePlacementsCount = computed(() =>
    this.placements().filter(p => p.statut === 'ACTIF').length
  );

  ngOnInit(): void {
    this.loadPlacements();
  }

  /* ==================== Chargement ==================== */

  loadPlacements(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.investmentService.getTousLesPlacements().subscribe({
      next: (placements) => {
        this.placements.set(placements || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger l\'historique des placements.');
        this.loading.set(false);
      }
    });
  }

  /* ==================== Filtres ==================== */

  updateSearch(value: string): void      { this.search.set(value); }
  updateStatusFilter(value: string): void {
    this.statusFilter.set(value as 'TOUS' | StatutPlacement);
  }

  /* ==================== Actions ==================== */

  cloturerPlacement(placement: Placement): void {
    if (!placement?.id || placement.statut !== 'ACTIF') return;
    const montant = Number(placement.valeurEstimee ?? 0).toFixed(2);
    if (!confirm(
      `Clôturer "${placement.nomFonds}" ?\n\n` +
      `Montant restitué : ${montant} €\n` +
      `Règle : capital + gain total à échéance. Aucun frais.`
    )) return;

    this.closingId.set(placement.id);
    this.error.set('');
    this.success.set('');
    this.investmentService.cloturerPlacement(placement.id).subscribe({
      next: (updated) => {
        this.placements.update(items =>
          items.map(i => i.id === updated.id ? updated : i)
        );
        this.operationsStore.addCloturePlacement(
          updated.numeroCompte,
          Number(updated.valeurEstimee ?? 0),
          updated.nomFonds
        );
        this.success.set(
          `✅ "${updated.nomFonds}" clôturé. ` +
          `${Number(updated.valeurEstimee ?? 0).toFixed(2)} € restitués.`
        );
        this.closingId.set(null);
      },
      error: () => {
        this.error.set('Impossible de clôturer ce placement.');
        this.closingId.set(null);
      }
    });
  }

  sortirAvantEcheance(placement: Placement): void {
    if (!placement?.id || placement.statut !== 'ACTIF') return;

    const capital      = Number(placement.montant ?? 0);
    const interets     = Number(placement.interetsCourus ?? 0);
    const frais        = this.calculerFraisSortie(placement);
    const recuperable  = capital + interets - frais;
    const explication  = this.getExplicationFrais(placement);

    if (!confirm(
      `Sortie anticipée de "${placement.nomFonds}" ?\n\n` +
      `Capital investi     : ${capital.toFixed(2)} €\n` +
      `Intérêts courus     : +${interets.toFixed(2)} €\n` +
      `Frais de sortie     : −${frais.toFixed(2)} € (${explication})\n` +
      `─────────────────────────────\n` +
      `Montant récupérable : ${recuperable.toFixed(2)} €\n\n` +
      `Les intérêts non courus sont perdus.`
    )) return;

    this.sortingId.set(placement.id);
    this.error.set('');
    this.success.set('');

    this.investmentService.sortirAvantEcheance(placement.id).subscribe({
      next: (updated) => {
        this.placements.update(items =>
          items.map(i => i.id === updated.id ? updated : i)
        );
        const fraisReels   = Number(updated.fraisSortie ?? 0);
        const interetsReel = Number(updated.interetsCourus ?? 0);
        const restitue     = Number(updated.montant ?? 0) + interetsReel - fraisReels;

        this.operationsStore.addSortieAnticipee(
          updated.numeroCompte,
          restitue,
          fraisReels,
          updated.nomFonds
        );
        this.success.set(
          `✅ Sortie effectuée pour "${updated.nomFonds}". ` +
          `Restitué : ${restitue.toFixed(2)} € ` +
          `(intérêts : +${interetsReel.toFixed(2)} € | frais : −${fraisReels.toFixed(2)} €)`
        );
        this.sortingId.set(null);
      },
      error: () => {
        this.error.set('Impossible d\'effectuer la sortie anticipée.');
        this.sortingId.set(null);
      }
    });
  }

  /* ==================== Helpers frais dégressifs ==================== */

  /**
   * Logique bancaire dégressive côté frontend (miroir du backend).
   * frais = max(capital × 0.25%,  capital × 2% × ratioRestant)
   */
  calculerFraisSortie(placement: Placement): number {
    const capital = Number(placement.montant ?? 0);
    const fraisMinimum = capital * 0.0025;

    if (!placement.datePlacement || !placement.dateCloture) {
      return Math.max(capital * 0.02, fraisMinimum);
    }

    const debut    = new Date(placement.datePlacement).getTime();
    const fin      = new Date(placement.dateCloture).getTime();
    const now      = Date.now();
    const totaux   = (fin - debut) / 86400000;
    const restants = Math.max(0, (fin - now) / 86400000);

    if (totaux <= 0) return parseFloat(fraisMinimum.toFixed(2));

    const ratio          = Math.min(1, restants / totaux);
    const fraisDegressifs = capital * 0.02 * ratio;
    return parseFloat(Math.max(fraisDegressifs, fraisMinimum).toFixed(2));
  }

  /**
   * Explication textuelle du taux de frais appliqué.
   */
  getExplicationFrais(placement: Placement): string {
    const capital = Number(placement.montant ?? 0);
    if (capital <= 0) return '';

    const frais = this.calculerFraisSortie(placement);
    const taux  = ((frais / capital) * 100).toFixed(2);

    if (!placement.datePlacement || !placement.dateCloture) {
      return `2.00% — taux plein (dates manquantes)`;
    }

    const fin      = new Date(placement.dateCloture).getTime();
    const now      = Date.now();
    const restants = Math.max(0, Math.round((fin - now) / 86400000));

    if (frais <= capital * 0.0026) {
      return `${taux}% — minimum garanti · J-${restants} avant échéance`;
    }
    return `${taux}% dégressif · ${restants} jours restants`;
  }

  calculerMontantRecuperableActif(placement: Placement): number {
    const capital  = Number(placement.montant ?? 0);
    const interets = Number(placement.interetsCourus ?? 0);
    const frais    = this.calculerFraisSortie(placement);
    return capital + interets - frais;
  }

  getMontantRecuperable(placement: Placement): number {
    if (placement.statut === 'ACTIF') {
      return this.calculerMontantRecuperableActif(placement);
    }
    if ((placement.fraisSortie ?? 0) > 0) {
      return Number(placement.montant ?? 0)
        + Number(placement.interetsCourus ?? 0)
        - Number(placement.fraisSortie ?? 0);
    }
    return Number(placement.valeurEstimee ?? placement.montant ?? 0);
  }

  getExplicationMontant(placement: Placement): string {
    if (placement.statut === 'ACTIF') {
      return 'Capital + intérêts courus − frais dégressifs';
    }
    if ((placement.fraisSortie ?? 0) > 0) {
      return 'Sortie anticipée — frais déduits';
    }
    return 'Clôture normale à échéance';
  }

  calculerGain(placement: Placement): number {
    return Number(placement.gainPrevu ?? 0);
  }

  statusLabel(statut: StatutPlacement): string {
    switch (statut) {
      case 'ACTIF':   return 'Actif';
      case 'CLOTURE': return 'Clôturé';
      case 'ANNULE':  return 'Annulé';
      default:        return statut;
    }
  }

  retour(): void {
    this.router.navigate(['/investissements']);
  }
}
