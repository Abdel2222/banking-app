import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
}

interface Compte {
  numCompte: string;
  intitule: string;
  balance: number;
}

@Component({
  selector: 'app-operations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './operations.component.html',
  styleUrls: ['./operations.component.scss']
})
export class OperationsComponent implements OnInit {
  operations = signal<Operation[]>([]);
  comptes = signal<Compte[]>([]);
  compteSelectionne = signal<string>('');
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  Math = Math;

  private operationsStore = inject(OperationsStoreService);

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const numCompteFromUrl = params['numCompte'];
      
      if (numCompteFromUrl) {
        console.log('NumCompte reçu depuis URL:', numCompteFromUrl);
        this.compteSelectionne.set(numCompteFromUrl);
      }
      
      this.loadComptes();
    });
  }

  loadComptes() {
    const token = localStorage.getItem('auth_token');

    this.http.get<any>('/api/comptes', {
      headers: { 'Authorization': `Bearer ${token}` }
    }).subscribe({
      next: (response) => {
        const comptes = Array.isArray(response) ? response : [response];
        this.comptes.set(comptes);

        if (this.compteSelectionne()) {
          this.loadOperations(this.compteSelectionne());
        } else if (comptes.length > 0) {
          this.compteSelectionne.set(comptes[0].numCompte);
          this.loadOperations(comptes[0].numCompte);
        }
      },
      error: (err) => {
        console.error('Erreur chargement comptes:', err);
        this.error.set('Impossible de charger les comptes');
      }
    });
  }

  loadOperations(numCompte: string) {
    this.loading.set(true);
    this.error.set(null);

    console.log('📊 [OPERATIONS] Chargement opérations pour:', numCompte);

    const localOps = this.operationsStore.getOperationsByCompte(numCompte);
    
    console.log('📊 [OPERATIONS] Opérations depuis le store local:', localOps);
    
    if (localOps.length > 0) {
      this.operations.set(localOps);
      this.loading.set(false);
    } else {
      this.error.set(null);
      this.operations.set([]);
      this.loading.set(false);
    }
  }

  onCompteChange(numCompte: string) {
    this.compteSelectionne.set(numCompte);
    this.loadOperations(numCompte);
  }

  exportPDF() {
    try {
      console.log('🚀 [PDF] Début génération PDF');
      
      import('jspdf').then(({ jsPDF }) => {
        try {
          console.log('✅ [PDF] jsPDF chargé avec succès');
          
          const doc = new jsPDF();
          
          const numCompte = this.compteSelectionne();
          const compteInfo = this.comptes().find(c => c.numCompte === numCompte);
          const dateExtrait = new Date().toLocaleDateString('fr-FR');
          const operations = this.operations();

          console.log('📊 [PDF] Nombre d\'opérations:', operations.length);

          // EN-TÊTE
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

          // INFORMATIONS
          doc.setTextColor(0, 0, 0);
          doc.setFontSize(11);
          doc.setFont('helvetica', 'bold');
          doc.text('Informations du compte', 14, 50);
          
          doc.setFont('helvetica', 'normal');
          doc.setFontSize(10);
          doc.text('Compte : ' + (compteInfo?.intitule || 'Compte'), 14, 58);
          doc.text('Numero : ' + numCompte, 14, 64);
          doc.text('Solde : ' + this.formatCurrency(compteInfo?.balance || 0), 14, 70);
          doc.text('Date : ' + dateExtrait, 14, 76);

          // TABLEAU
          let y = 90;

          doc.setFillColor(0, 255, 157);
          doc.rect(14, y, 182, 8, 'F');
          doc.setTextColor(10, 14, 39);
          doc.setFontSize(9);
          doc.setFont('helvetica', 'bold');
          doc.text('Date', 16, y + 5);
          doc.text('Type', 46, y + 5);
          doc.text('Description', 76, y + 5);
          doc.text('Montant', 170, y + 5);

          y += 10;

          if (operations.length === 0) {
            doc.setTextColor(150, 150, 150);
            doc.setFont('helvetica', 'normal');
            doc.text('Aucune operation disponible', 14, y);
          } else {
            console.log('📝 [PDF] Génération des lignes du tableau...');
            
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

              const desc = (op.description || op.communication || '-').substring(0, 25);
              doc.text(desc, 76, y);

              const montantValue = Math.abs(op.montant);
              const montantText = (op.montant > 0 ? '+' : '-') + montantValue.toFixed(2) + ' EUR';
              
              if (op.montant > 0) {
                doc.setTextColor(0, 200, 0);
              } else {
                doc.setTextColor(200, 0, 0);
              }
              
              doc.setFont('helvetica', 'bold');
              const textWidth = doc.getTextWidth(montantText);
              doc.text(montantText, 190 - textWidth, y);

              y += 8;

              if (y > 270) {
                doc.addPage();
                y = 20;
              }
            });

            console.log('✅ [PDF] Lignes générées avec succès');

            // RÉSUMÉ
            y += 10;

            const totalDepots = operations
              .filter(op => op.montant > 0)
              .reduce((sum, op) => sum + op.montant, 0);
            
            const totalRetraits = operations
              .filter(op => op.montant < 0)
              .reduce((sum, op) => sum + Math.abs(op.montant), 0);

            doc.setFont('helvetica', 'bold');
            doc.setFontSize(11);
            doc.setTextColor(0, 0, 0);
            doc.text('Resume', 14, y);
            
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(10);
            doc.setTextColor(0, 150, 0);
            doc.text('Total entrees : +' + this.formatCurrency(totalDepots), 14, y + 8);
            
            doc.setTextColor(200, 0, 0);
            doc.text('Total sorties : -' + this.formatCurrency(totalRetraits), 14, y + 14);
            
            doc.setTextColor(0, 0, 0);
            doc.setFont('helvetica', 'bold');
            doc.text(operations.length + ' operation(s)', 14, y + 20);
          }

          // PIED DE PAGE
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

          console.log('✅ [PDF] Document PDF créé avec succès');

          const fileName = 'extrait_' + numCompte + '_' + new Date().toISOString().split('T')[0] + '.pdf';
          doc.save(fileName);
          
          console.log('✅ [PDF] PDF téléchargé:', fileName);
          
        } catch (error: any) {
          console.error('❌ [PDF] Erreur:', error);
          console.error('Stack:', error.stack);
          alert('Erreur lors de la génération du PDF: ' + error.message);
        }
      }).catch(importError => {
        console.error('❌ [PDF] Erreur import jsPDF:', importError);
        alert('Impossible de charger la bibliothèque jsPDF');
      });
    } catch (outerError: any) {
      console.error('❌ [PDF] Erreur générale export PDF:', outerError);
      alert('Erreur générale: ' + outerError.message);
    }
  }

  getTypeIcon(type: string): string {
    switch(type?.toUpperCase()) {
      case 'VIREMENT': return '💸';
      case 'DEPOT': return '💰';
      case 'RETRAIT': return '🏧';
      case 'FRAIS': return '💳';
      default: return '📝';
    }
  }

  getStatutDisplay(operation: Operation): string {
    const opDate = new Date(operation.dateOperation);
    const now = new Date();
    
    // Si la date est future, afficher "PLANIFIÉ"
    if (opDate > now) {
      return 'PLANIFIÉ';
    }
    
    // Sinon afficher le statut original
    return operation.statut || 'COMPLETED';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDateShort(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit'
    });
  }

  retourDashboard() {
    this.router.navigate(['/dashboard']);
  }
}