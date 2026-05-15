import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  InvestmentService,
  Placement,
  StatutPlacement
} from '../investment.service';

@Component({
  selector: 'app-investments-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './investments-history.component.html',
  styleUrl: './investments-history.component.scss'
})
export class InvestmentsHistoryComponent implements OnInit {
  private readonly investmentService = inject(InvestmentService);
  private readonly router = inject(Router);

  placements = signal<Placement[]>([]);
  search = signal<string>('');
  statusFilter = signal<'TOUS' | StatutPlacement>('TOUS');

  loading = signal<boolean>(false);
  closingId = signal<number | null>(null);
  error = signal<string>('');
  success = signal<string>('');

  filteredPlacements = computed(() => {
    const query = this.search().trim().toLowerCase();
    const statut = this.statusFilter();

    return this.placements().filter((placement) => {
      const matchSearch =
        !query ||
        placement.nomFonds?.toLowerCase().includes(query) ||
        placement.numeroCompte?.toLowerCase().includes(query) ||
        placement.clientNomComplet?.toLowerCase().includes(query);

      const matchStatus =
        statut === 'TOUS' || placement.statut === statut;

      return matchSearch && matchStatus;
    });
  });

  totalCapitalInvesti = computed(() => {
    return this.placements()
      .filter((placement) => placement.statut === 'ACTIF')
      .reduce((total, placement) => total + Number(placement.montant || 0), 0);
  });

  totalGainPrevu = computed(() => {
    return this.placements()
      .filter((placement) => placement.statut === 'ACTIF')
      .reduce((total, placement) => total + Number(placement.gainPrevu || 0), 0);
  });

  activePlacementsCount = computed(() => {
    return this.placements()
      .filter((placement) => placement.statut === 'ACTIF')
      .length;
  });

  ngOnInit(): void {
    this.loadPlacements();
  }

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
        this.error.set('Impossible de charger l’historique des placements.');
        this.loading.set(false);
      }
    });
  }

  updateSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.search.set(input.value);
  }

  updateStatusFilter(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value as 'TOUS' | StatutPlacement);
  }

  cloturerPlacement(placement: Placement): void {
    if (!placement?.id || placement.statut !== 'ACTIF') {
      return;
    }

    const confirmation = confirm(
      `Voulez-vous vraiment clôturer le placement "${placement.nomFonds}" ?`
    );

    if (!confirmation) {
      return;
    }

    this.closingId.set(placement.id);
    this.error.set('');
    this.success.set('');

    this.investmentService.cloturerPlacement(placement.id).subscribe({
      next: (updatedPlacement) => {
        this.placements.update((items) =>
          items.map((item) =>
            item.id === updatedPlacement.id ? updatedPlacement : item
          )
        );

        this.success.set('Placement clôturé avec succès.');
        this.closingId.set(null);
      },
      error: () => {
        this.error.set('Impossible de clôturer ce placement.');
        this.closingId.set(null);
      }
    });
  }

  statusLabel(statut: StatutPlacement): string {
    switch (statut) {
      case 'ACTIF':
        return 'Actif';
      case 'CLOTURE':
        return 'Clôturé';
      case 'ANNULE':
        return 'Annulé';
      default:
        return statut;
    }
  }

  retour(): void {
    this.router.navigate(['/investissements']);
  }
}
