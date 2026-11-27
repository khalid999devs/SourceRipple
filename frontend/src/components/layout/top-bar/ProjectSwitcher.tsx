import { ChevronsUpDown, FolderKanban } from 'lucide-react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/form/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/overlay/popover';

export function ProjectSwitcher() {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="hidden text-muted-foreground sm:inline-flex">
          <FolderKanban aria-hidden />
          All projects
          <ChevronsUpDown aria-hidden />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="flex w-64 flex-col gap-3 text-sm">
        <p className="text-muted-foreground">Projects you create will show up here.</p>
        <Button asChild variant="secondary" size="sm">
          <Link to="/app/projects">Go to projects</Link>
        </Button>
      </PopoverContent>
    </Popover>
  );
}
