import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { UiKitDemoDialogComponent } from './ui-kit-demo-dialog.component';

interface DemoShipment {
  shipmentId: string;
  route: string;
  status: 'approved' | 'submitted' | 'rejected' | 'stage1';
  eta: string;
}

@Component({
  selector: 'app-ui-kit',
  templateUrl: './ui-kit.component.html',
  styleUrls: ['./ui-kit.component.scss'],
  standalone: false,
})
export class UiKitComponent implements AfterViewInit {
  @ViewChild(MatSort) sort!: MatSort;

  readonly displayedColumns = ['shipmentId', 'route', 'status', 'eta'];
  readonly tableData = new MatTableDataSource<DemoShipment>([
    { shipmentId: 'SHP-10023', route: 'Harare -> Bulawayo', status: 'approved', eta: '14:30' },
    { shipmentId: 'SHP-10041', route: 'Mutare -> Harare', status: 'stage1', eta: '17:20' },
    { shipmentId: 'SHP-10056', route: 'Gweru -> Beitbridge', status: 'submitted', eta: '11:15' },
    { shipmentId: 'SHP-10071', route: 'Masvingo -> Bulawayo', status: 'rejected', eta: '09:05' },
  ]);

  constructor(private readonly dialog: MatDialog) {}

  ngAfterViewInit(): void {
    this.tableData.sort = this.sort;
  }

  openModal(): void {
    this.dialog.open(UiKitDemoDialogComponent, {
      width: 'min(520px, 92vw)',
      autoFocus: 'first-tabbable',
    });
  }
}

