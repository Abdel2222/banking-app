import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { OperationsStoreService } from '../../core/services/operations-store.service';

interface Operation {
  id: number;
  type: string;
  montant: number;
  dateOperation: string;
  numCompteSource?: string;
  numCompteDestinataire?: string;
  communication?: string;
  description?: string;
  statut?: string;
  nomTitulaireDestinataire?: string;
}

interface Compte {
  numCompte: string;
  intitule: string;
  balance: number;
}

type PeriodePreset = 'all' | 'month' | '3months' | 'year' | 'custom';

@Component({
  selector: 'app-operations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './operations.component.html',
  styleUrls: ['./operations.component.scss']
})
export class OperationsComponent implements OnInit {
  operations = signal<Operation[]>([]);
  comptes = signal<Compte[]>([]);
  compteSelectionne = signal<string>('');
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  dateDebut = signal<string>('');
  dateFin = signal<string>('');
  periodeActive = signal<PeriodePreset>('all');

  operationsFiltrees = computed(() => {
    const all = this.operations();
    const debut = this.dateDebut();
    const fin = this.dateFin();
    if (!debut && !fin) return all;
    return all.filter(op => {
      const opDate = new Date(op.dateOperation);
      opDate.setHours(0, 0, 0, 0);
      if (debut) {
        const d = new Date(debut);
        d.setHours(0, 0, 0, 0);
        if (opDate < d) return false;
      }
      if (fin) {
        const f = new Date(fin);
        f.setHours(23, 59, 59, 999);
        if (opDate > f) return false;
      }
      return true;
    });
  });

  totalEntrees = computed(() =>
    this.operationsFiltrees().filter(op => op.montant > 0).reduce((sum, op) => sum + op.montant, 0)
  );

  totalSorties = computed(() =>
    this.operationsFiltrees().filter(op => op.montant < 0).reduce((sum, op) => sum + Math.abs(op.montant), 0)
  );

  Math = Math;

  private operationsStore = inject(OperationsStoreService);
  private readonly API_BASE = 'http://localhost:8084/api';

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const numCompteFromUrl = params['numCompte'];
      if (numCompteFromUrl) {
        this.compteSelectionne.set(numCompteFromUrl);
      }
      this.loadComptes();
    });
  }

  loadComptes() {
    const token = localStorage.getItem('auth_token');
    this.http.get<any>(`${this.API_BASE}/comptes`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).subscribe({
      next: (response) => {
        const comptes = Array.isArray(response) ? response : (response?.data ?? [response]);
        this.comptes.set(comptes);
        if (this.compteSelectionne()) {
          this.loadOperations(this.compteSelectionne());
        } else if (comptes.length > 0) {
          this.compteSelectionne.set(comptes[0].numCompte);
          this.loadOperations(comptes[0].numCompte);
        }
      },
      error: () => this.error.set('Impossible de charger les comptes')
    });
  }

  loadOperations(numCompte: string) {
    this.loading.set(true);
    this.error.set(null);
    const token = localStorage.getItem('auth_token');
    const params = new HttpParams().set('numCompte', numCompte).set('limit', '500');
    this.http.get<any>(`${this.API_BASE}/operations/recent`, {
      headers: { 'Authorization': `Bearer ${token}` },
      params
    }).subscribe({
      next: (response) => {
        const backendOps = Array.isArray(response) ? response : (response?.data ?? []);
        const normalizedBackend = backendOps.map((op: any) => this.normalizeOperation(op, numCompte));

        // ✅ Dédoublonnage par date + montant + type (gère les doublons en base)
        const seen = new Set<string>();
        const deduped = normalizedBackend.filter((op: Operation) => {
          const key = `${op.type}_${Math.abs(op.montant)}_${op.dateOperation?.toString().substring(0, 16)}`;
          if (seen.has(key)) return false;
          seen.add(key);
          return true;
        });

        const localOps = this.operationsStore.getOperationsByCompte(numCompte);
        const allOps = [...deduped];
        for (const localOp of localOps) {
          if (!allOps.find(o => o.id === localOp.id)) allOps.push(localOp);
        }
        allOps.sort((a, b) => new Date(b.dateOperation).getTime() - new Date(a.dateOperation).getTime());
        this.operations.set(allOps);
        this.loading.set(false);
      },
      error: () => {
        const localOps = this.operationsStore.getOperationsByCompte(numCompte);
        this.operations.set(localOps);
        this.loading.set(false);
      }
    });
  }

  private normalizeOperation(op: any, numCompteActuel?: string): Operation {
    let montant = Number(op?.montant ?? 0);
    const description = op?.description ?? op?.libelle ?? op?.type ?? 'Opération';
    const commentaire = op?.commentaire ?? '';
    const rawType = (op?.type ?? '').toString().toUpperCase();

    const communication = (
      op?.communication ??
      op?.communicationStructuree ??
      op?.communicationOGM ??
      op?.libelleCommunication ??
      commentaire ??
      ''
    ).trim();

    // ✅ Frais → toujours négatif
    const isFrais =
      description.toLowerCase().includes('frais') ||
      commentaire.toUpperCase().includes('FRAIS_GESTION') ||
      rawType === 'FRAIS' ||
      rawType === 'FRAIS_GESTION' ||
      rawType === 'BLOCAGE_CARTE';
    if (isFrais && montant > 0) montant = -montant;

    // ✅ Virement sortant → négatif
    if ((rawType === 'VIREMENT' || rawType === 'VIREMENT_INTERNE') && montant > 0) {
      const numCompteDest = op?.numeroCompteDestinataire ?? op?.numCompteDestinataire ?? '';
      const descLower = description.toLowerCase();
      if (descLower.includes('réception') || descLower.includes('reception')) {
        montant = Math.abs(montant);
      } else if (numCompteDest && numCompteDest !== numCompteActuel) {
        montant = -Math.abs(montant);
      }
    }

    let displayType = rawType || 'OPERATION';
    if (isFrais) displayType = 'FRAIS';

    return {
      id: op?.id ?? 0,
      type: displayType,
      montant,
      dateOperation: op?.dateOperation ?? op?.date ?? op?.createdAt ?? new Date().toISOString(),
      numCompteSource: op?.numCompteSource ?? op?.numeroCompte,
      numCompteDestinataire: op?.numCompteDestinataire ?? op?.numeroCompteDestinataire,
      communication,
      description,
      statut: op?.statut ?? 'COMPLETED',
      nomTitulaireDestinataire: op?.nomTitulaireDestinataire ?? ''
    };
  }

  onCompteChange(numCompte: string) {
    this.compteSelectionne.set(numCompte);
    this.loadOperations(numCompte);
  }

  setPeriode(periode: PeriodePreset) {
    this.periodeActive.set(periode);
    const now = new Date();
    switch (periode) {
      case 'all': this.dateDebut.set(''); this.dateFin.set(''); break;
      case 'month': {
        const debut = new Date(now.getFullYear(), now.getMonth(), 1);
        this.dateDebut.set(this.toInputDate(debut));
        this.dateFin.set(this.toInputDate(now));
        break;
      }
      case '3months': {
        const debut = new Date(now.getFullYear(), now.getMonth() - 3, now.getDate());
        this.dateDebut.set(this.toInputDate(debut));
        this.dateFin.set(this.toInputDate(now));
        break;
      }
      case 'year': {
        const debut = new Date(now.getFullYear(), 0, 1);
        this.dateDebut.set(this.toInputDate(debut));
        this.dateFin.set(this.toInputDate(now));
        break;
      }
      case 'custom': break;
    }
  }

  onDateDebutChange(value: string) { this.dateDebut.set(value); this.periodeActive.set('custom'); }
  onDateFinChange(value: string) { this.dateFin.set(value); this.periodeActive.set('custom'); }
  resetDates() { this.dateDebut.set(''); this.dateFin.set(''); this.periodeActive.set('all'); }

  private toInputDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  isCommunicationStructuree(communication: string | undefined): boolean {
    if (!communication) return false;
    return /^\+{3}\d{3}\/\d{4}\/\d{5}\+{3}$/.test(communication.trim());
  }

  getLibelleAffichage(op: Operation): string {
    if (op.communication && op.communication.trim()) return op.communication.trim();
    return op.description || op.type || 'Opération';
  }

  // ✅ Numéro masqué uniquement — pas de nom
  getContrepartie(op: Operation): string {
    const monCompte = this.compteSelectionne();
    if (op.type === 'VIREMENT' || op.type === 'VIREMENT_INTERNE') {
      if (op.numCompteSource === monCompte && op.numCompteDestinataire) {
        return `Vers ${this.formatIban(op.numCompteDestinataire)}`;
      }
      if (op.numCompteDestinataire === monCompte && op.numCompteSource) {
        return `De ${this.formatIban(op.numCompteSource)}`;
      }
      if (op.montant < 0 && op.numCompteDestinataire) {
        return `Vers ${this.formatIban(op.numCompteDestinataire)}`;
      }
      if (op.montant > 0 && op.numCompteSource) {
        return `De ${this.formatIban(op.numCompteSource)}`;
      }
    }
    return '';
  }

  formatIban(num: string): string {
    if (!num || num.length < 4) return num;
    return '••• ' + num.slice(-4);
  }

  exportPDF() {
    try {
      import('jspdf').then(({ jsPDF }) => {
        try {
          const doc = new jsPDF();
          const numCompte = this.compteSelectionne();
          const compteInfo = this.comptes().find(c => c.numCompte === numCompte);
          const dateExtrait = new Date().toLocaleDateString('fr-FR');
          const operations = this.operationsFiltrees();

          doc.setFillColor(10, 14, 39);
          doc.rect(0, 0, 210, 40, 'F');
          doc.setTextColor(0, 255, 157);
          doc.setFontSize(24);
          doc.setFont('helvetica', 'bold');
          doc.text('TECHNO-BANK', 14, 20);
          doc.setTextColor(255, 255, 255);
          doc.setFontSize(12);
          doc.setFont('helvetica', 'normal');
          doc.text('Extrait de compte', 14, 30);

          doc.setTextColor(0, 0, 0);
          doc.setFontSize(11);
          doc.setFont('helvetica', 'bold');
          doc.text('Informations du compte', 14, 50);
          doc.setFont('helvetica', 'normal');
          doc.setFontSize(10);
          doc.text('Compte : ' + (compteInfo?.intitule || 'Compte'), 14, 58);
          doc.text('Numero : ' + numCompte, 14, 64);
          doc.text('Solde : ' + this.formatCurrency(compteInfo?.balance || 0), 14, 70);
          doc.text('Date d\'edition : ' + dateExtrait, 14, 76);

          let periodeText = 'Periode : Toutes les operations';
          if (this.dateDebut() || this.dateFin()) {
            const debut = this.dateDebut() ? this.formatDateShort(this.dateDebut()) : '...';
            const fin = this.dateFin() ? this.formatDateShort(this.dateFin()) : 'aujourd\'hui';
            periodeText = `Periode : du ${debut} au ${fin}`;
          }
          doc.setFont('helvetica', 'italic');
          doc.text(periodeText, 14, 82);

          let y = 96;
          doc.setFillColor(0, 255, 157);
          doc.rect(14, y, 182, 8, 'F');
          doc.setTextColor(10, 14, 39);
          doc.setFontSize(9);
          doc.setFont('helvetica', 'bold');
          doc.text('Date', 16, y + 5);
          doc.text('Type', 46, y + 5);
          doc.text('Libelle / Communication', 76, y + 5);
          doc.text('Montant', 170, y + 5);
          y += 10;

          if (operations.length === 0) {
            doc.setTextColor(150, 150, 150);
            doc.setFont('helvetica', 'normal');
            doc.text('Aucune operation sur la periode selectionnee', 14, y);
          } else {
            operations.forEach((op, index) => {
              if (index % 2 === 0) {
                doc.setFillColor(245, 245, 245);
                doc.rect(14, y - 5, 182, 8, 'F');
              }
              doc.setFont('helvetica', 'normal');
              doc.setFontSize(8);
              doc.setTextColor(0, 0, 0);
              doc.text(this.formatDateShort(op.dateOperation), 16, y);
              doc.text(op.type || 'N/A', 46, y);
              const libelle = this.getLibelleAffichage(op).substring(0, 45);
              doc.text(libelle, 76, y);
              const montantValue = Math.abs(op.montant);
              const montantText = (op.montant > 0 ? '+' : '-') + montantValue.toFixed(2) + ' EUR';
              if (op.montant > 0) doc.setTextColor(0, 200, 0);
              else doc.setTextColor(200, 0, 0);
              doc.setFont('helvetica', 'bold');
              const textWidth = doc.getTextWidth(montantText);
              doc.text(montantText, 190 - textWidth, y);
              y += 8;
              if (y > 270) { doc.addPage(); y = 20; }
            });

            y += 10;
            const totalDepots = operations.filter(op => op.montant > 0).reduce((sum, op) => sum + op.montant, 0);
            const totalRetraits = operations.filter(op => op.montant < 0).reduce((sum, op) => sum + Math.abs(op.montant), 0);
            doc.setFont('helvetica', 'bold');
            doc.setFontSize(11);
            doc.setTextColor(0, 0, 0);
            doc.text('Resume de la periode', 14, y);
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(10);
            doc.setTextColor(0, 150, 0);
            doc.text('Total entrees : +' + this.formatCurrency(totalDepots), 14, y + 8);
            doc.setTextColor(200, 0, 0);
            doc.text('Total sorties : -' + this.formatCurrency(totalRetraits), 14, y + 14);
            doc.setTextColor(0, 0, 0);
            doc.setFont('helvetica', 'bold');
            doc.text(operations.length + ' operation(s) sur la periode', 14, y + 20);
          }

          const pageCount = (doc as any).internal.getNumberOfPages();
          for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            doc.setFontSize(8);
            doc.setTextColor(150, 150, 150);
            doc.setFont('helvetica', 'normal');
            const footerText = 'Page ' + i + '/' + pageCount + ' - Techno-Bank - ' + dateExtrait;
            const footerWidth = doc.getTextWidth(footerText);
            doc.text(footerText, (210 - footerWidth) / 2, doc.internal.pageSize.height - 10);
          }

          let suffixe = new Date().toISOString().split('T')[0];
          if (this.dateDebut() && this.dateFin()) {
            suffixe = `${this.dateDebut()}_au_${this.dateFin()}`;
          }
          doc.save('extrait_' + numCompte + '_' + suffixe + '.pdf');
        } catch (error: any) {
          alert('Erreur lors de la génération du PDF: ' + error.message);
        }
      }).catch(() => alert('Impossible de charger la bibliothèque jsPDF'));
    } catch (outerError: any) {
      alert('Erreur générale: ' + outerError.message);
    }
  }

  getTypeIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'VIREMENT':
      case 'VIREMENT_INTERNE': return '💸';
      case 'DEPOT': return '💰';
      case 'RETRAIT': return '🏧';
      case 'FRAIS':
      case 'FRAIS_GESTION': return '💳';
      default: return '📝';
    }
  }

  getStatutDisplay(operation: Operation): string {
    const opDate = new Date(operation.dateOperation);
    const now = new Date();
    if (opDate > now) return 'PLANIFIÉ';
    return operation.statut || 'COMPLETED';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(amount);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatDateShort(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: '2-digit'
    });
  }

  retourDashboard() { this.router.navigate(['/dashboard']); }
}