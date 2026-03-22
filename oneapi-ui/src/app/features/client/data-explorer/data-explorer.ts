import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetadataTreeExplorer } from '../../../shared/components/metadata-tree-explorer/metadata-tree-explorer';

@Component({
  selector: 'app-data-explorer',
  standalone: true,
  imports: [
    CommonModule,
    MetadataTreeExplorer
  ],
  template: '<app-metadata-tree-explorer [autoLoadSources]="true"></app-metadata-tree-explorer>',
  styles: [':host { display: block; height: 100%; }']
})
export class DataExplorer {}
