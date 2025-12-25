import React from 'react';
import { TableDataDictionary } from './table-data-dictionary/table-data-dictionary';

export interface DataDictionarySectionProps {}

export const DataDictionarySection: React.FC<DataDictionarySectionProps> = () => {
    return (
        <section
            className="flex flex-1 flex-col overflow-hidden px-2"
            data-vaul-no-drag
        >
            <div className="flex flex-1 flex-col overflow-hidden">
                <TableDataDictionary />
            </div>
        </section>
    );
};
