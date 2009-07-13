#!/usr/bin/env perl

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

use FileHandle;

my $dryrun = 0;
my $quiet = 0;

#
# Usage: notice.pl [-n] [-q] files...
#

my $NOTICE = <<'EOT';
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
EOT

# Note: source code notices must not be changed by code formatter

my $NOTICE_java = $NOTICE;
$NOTICE_java =~ s/^/ * /gm;
$NOTICE_java = "/*\n" . $NOTICE_java . " */\n\n";

my $NOTICE_xml = $NOTICE;
$NOTICE_xml =~ s/^/  /gm;
$NOTICE_xml = "<!--\n" . $NOTICE_xml . "-->\n";

my $NOTICE_sh = $NOTICE;
$NOTICE_sh =~ s/^/# /gm;

my $NOTICE_bat = $NOTICE;
$NOTICE_bat =~ s/^/:: /gm;

sub addNotice {
    my ($fh, $file, $notice) = @_;
    print "$file\n";

    return if ($dryrun);

    my $tmp = "$file.tmp";
    my $fh2 = new FileHandle(">$tmp");

    print $fh2 $notice;
    while (<$fh>) {
	print $fh2 $_;
    }

    unlink($file);
    rename($tmp, $file);
}

#
# locate existing copyright & license
#	starts at top of file
#	ends at "package" statement
#
sub notice_java {
    my $file = shift;
    my $fh = new FileHandle($file);
    my $header;
    my $package = undef;

    while (<$fh>) {
        if (/^package\s+[\w.]+\s*;/) {
	    $package = $_;
	    last;
	}
	last if ($. > 200);
	$header .= $_;
    }

    if (! $package) {
        print STDERR "$file: ERROR: no package statement.\n";
	return;
    }

    return if ($header eq $NOTICE_java);

    # completely replace all before package statement
    # so DON'T change below, without changing above
    # to stop at end-of-first-comment.
    $header = '';
    #$header = '' if ($header =~ /copyright/im);
    #$header = "\n" . $header unless ($header =~ /^\n/);

    addNotice($fh, $file, $NOTICE_java . $header . $package);
}

sub notice_xml {
    my $file = shift;
    my $fh = new FileHandle($file);
    my $header = undef;
    my $decl = qq(<?xml version="1.0"?>\n);
    my $end = 0;
    my $start = 0;

    while (<$fh>) {
    	if ($. == 1 && /^\<\?/) {
	    $decl = $_;
	    next;
	}

	$header .= $_;

    	if (!$start) {
	  last unless (/^<!--/);
	  $start = 1;
	}

        if (/-->/) {
	    $end = 1;
	    last;
	}
    }

    return if ($header eq $NOTICE_xml);

    $header = '' if ($header =~ /copyright/im);

    if ($start && !$end) {
        print STDERR "$file: ERROR: initial comment not terminated.\n";
	return;
    }

    addNotice($fh, $file, $decl . $NOTICE_xml . $header);
}

sub notice_sh {
    my $file = shift;
    my $fh = new FileHandle($file);
    my $header = undef;
    my $hashbang = undef;
    my $end = '';
    my $start = '';

    while (<$fh>) {
    	if ($. == 1 && /^#!/) {
	    $hashbang = $_;
	    next;
	}

    	if (!$start) {
	  unless (/^(#|\s*$)/) {
	    $header = $_;
	    last;
	  }
	  $start = 1;
	  next if (/^\s*$/);
	}

        if (!/^#/) {
	    $end = $_;
	    last;
	}

	$header .= $_;
    }

    return if ($header eq $NOTICE_sh);

    $hashbang .= "\n" if ($hashbang);

    $header = '' if ($header =~ /copyright/im);
    $header .= $end;
    $header = "\n" . $header unless ($header =~ /^\n/);

    if ($start && !$end) {
        print STDERR "$file: ERROR: initial comment not terminated.\n";
	return;
    }

    addNotice($fh, $file, $hashbang . $NOTICE_sh . $header);
}

sub notice_bat {
    my $file = shift;
    my $fh = new FileHandle($file);
    my $header = undef;
    my $atecho = undef;
    my $end = '';
    my $start = '';

    while (<$fh>) {
	s/\r\n$/\n/;
    	if ($. == 1 && /^\@echo/) {
	    $atecho = $_;
	    next;
	}

    	if (!$start) {
	  unless (/^(::|\s*$)/) {
	    $header = $_;
	    last;
	  }
	  $start = 1;
	  next if (/^\s*$/);
	}

        if (!/^::/) {
	    $end = $_;
	    last;
	}

	$header .= $_;
    }

    return if ($header eq $NOTICE_bat);

    $atecho .= "\n" if ($atecho);

    $header = '' if ($header =~ /copyright/im);
    $header .= $end;
    $header = "\n" . $header unless ($header =~ /^\n/);

    if ($start && !$end) {
        print STDERR "$file: ERROR: initial comment not terminated.\n";
	return;
    }

    $header = $atecho . $NOTICE_bat . $header;
    $header =~ s/\n/\r\n/mg;
    addNotice($fh, $file, $header);
}

sub notice_unknown {
    my $file = shift;
    my $fh = new FileHandle($file);

    my $line = <$fh>;

    if ($line =~ /^#!/) {
        notice_sh($file);
    }
    elsif ($line =~ /^\<\?xml/) {
        notice_xml($file);
    }
    elsif (! $quiet) {
	print STDERR "$file: unknown file type.\n";
    }
}

foreach my $f (@ARGV) {
    if ($f eq '-n') {
	$dryrun++;
    }
    elsif ($f eq '-q') {
	$quiet++;
    }
    elsif ($f =~ /\.java$/) {
	notice_java($f);
    }
    elsif ($f =~ /\.(xml|xsd|system|composite)$/) {
	notice_xml($f);
    }
    elsif ($f =~ /\.(sh|app|ini|xargs)$/) {
	notice_sh($f);
    }
    elsif ($f =~ /\.(bat)$/) {
	notice_bat($f);
    }
    else {
	notice_unknown($f);
    }
}

