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

  /* ====== Computed ====== */

  filteredPlacements = computed(() => {
    const query  = this.search().trim().toLowerCase();
    const statut = this.statusFilter();
    return this.placements().filter(p => {
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
      .reduce((t, p) => t + Number(p.montant || 0), 0)
  );

  totalGainPrevu = computed(() =>
    this.placements()
      .filter(p => p.statut === 'ACTIF')
      .reduce((t, p) => t + Number(p.gainPrevu || 0), 0)
  );

  activePlacementsCount = computed(() =>
    this.placements().filter(p => p.statut === 'ACTIF').length
  );

  /* ====== Lifecycle ====== */

  ngOnInit(): void {
    this.loadPlacements();
  }

  /* ====== Chargement ====== */

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

  /* ====== Filtres ====== */

  updateSearch(value: string): void {
    this.search.set(value);
  }

  updateStatusFilter(value: string): void {
    this.statusFilter.set(value as 'TOUS' | StatutPlacement);
  }

  /* ====== Clôture normale (gain inclus) ====== */

  cloturerPlacement(placement: Placement): void {
    if (!placement?.id || placement.statut !== 'ACTIF') return;

    const valeur = Number(placement.valeurEstimee ?? 0).toFixed(2);
    if (!confirm(
      `Clôturer "${placement.nomFonds}" ?\n` +
      `💰 Montant restitué : ${valeur} € (capital + gain)`
    )) return;

    this.closingId.set(placement.id);
    this.error.set('');
    this.success.set('');

    this.investmentService.cloturerPlacement(placement.id).subscribe({
      next: (updated) => {
        this.placements.update(items =>
          items.map(i => i.id === updated.id ? updated : i)
        );

        // ✅ Enregistrer dans le store d'opérations
        this.operationsStore.addCloturePlacement(
          updated.numeroCompte,
          Number(updated.valeurEstimee ?? 0),
          updated.nomFonds
        );

        this.success.set(
          `✅ Placement "${updated.nomFonds}" clôturé. ` +
          `${Number(updated.valeurEstimee ?? 0).toFixed(2)} € restitués sur votre compte.`
        );
        this.closingId.set(null);
      },
      error: () => {
        this.error.set('Impossible de clôturer ce placement.');
        this.closingId.set(null);
      }
    });
  }

  /* ====== Sortie anticipée (frais 2%, gain perdu) ====== */

  sortirAvantEcheance(placement: Placement): void {
    if (!placement?.id || placement.statut !== 'ACTIF') return;

    const fraisEstimes = (Number(placement.montant) * 0.02).toFixed(2);
    const retourEstime = (Number(placement.montant) - Number(fraisEstimes)).toFixed(2);

    if (!confirm(
      `Sortie anticipée de "${placement.nomFonds}" ?\n` +
      `⚠️ Frais de sortie : ${fraisEstimes} € (2%)\n` +
      `💰 Montant restitué : ${retourEstime} €\n` +
      `❌ Le gain prévu est perdu.`
    )) return;

    this.sortingId.set(placement.id);
    this.error.set('');
    this.success.set('');

    this.investmentService.sortirAvantEcheance(placement.id).subscribe({
      next: (updated) => {
        this.placements.update(items =>
          items.map(i => i.id === updated.id ? updated : i)
        );

        // ✅ Valeurs sécurisées — évite NaN / undefined
        const frais    = Number(updated.fraisSortie ?? 0);
        const restitue = Number(updated.montant ?? 0) - frais;

        // ✅ Enregistrer dans le store d'opérations
        this.operationsStore.addSortieAnticipee(
          updated.numeroCompte,
          restitue,
          frais,
          updated.nomFonds
        );

        this.success.set(
          `✅ Sortie anticipée effectuée. ` +
          `Frais : ${frais.toFixed(2)} €. ` +
          `Restitué : ${restitue.toFixed(2)} €`
        );
        this.sortingId.set(null);
      },
      error: () => {
        this.error.set('Impossible d\'effectuer la sortie anticipée.');
        this.sortingId.set(null);
      }
    });
  }

  /* ====== Helpers ====== */

  /**
   * Calcule le montant récupérable selon le statut :
   * - ACTIF         → montant + gainPrevu
   * - Sortie antic. → montant - fraisSortie
   * - Clôturé/Annulé → valeurEstimee du backend
   */
  getMontantRecuperable(placement: Placement): number {
    if (placement.fraisSortie != null && Number(placement.fraisSortie) > 0) {
      return Number(placement.montant) - Number(placement.fraisSortie);
    }
    return Number(placement.valeurEstimee ?? placement.montant ?? 0);
  }

  /**
   * Calcule le gain en euros directement depuis montant × rendement.
   * Utilisé en fallback si gainPrevu n'est pas fourni par le backend.
   */
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