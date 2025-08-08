#!/usr/bin/env python3
"""
Enhanced AWS Usage Analysis Script
Generates comprehensive visualizations and reports from AWS usage data
"""

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from matplotlib.ticker import FuncFormatter
import seaborn as sns
import numpy as np
from datetime import datetime
import warnings

# Suppress future warnings
warnings.simplefilter(action='ignore', category=FutureWarning)

# Set global style parameters
sns.set_style("whitegrid")
plt.rcParams['figure.facecolor'] = 'white'
plt.rcParams['axes.grid'] = True
plt.rcParams['grid.alpha'] = 0.3
plt.rcParams['font.size'] = 12
plt.rcParams['axes.titlesize'] = 14
plt.rcParams['axes.labelsize'] = 12


# Formatting functions
def human_format(num):
    """Convert numbers to human-readable format (K, M, etc.)"""
    magnitude = 0
    while abs(num) >= 1000:
        magnitude += 1
        num /= 1000.0
    return '%.2f%s' % (num, ['', 'K', 'M', 'B'][magnitude])


def load_and_clean_data(filepath):
    """Load and preprocess the AWS usage data"""
    try:
        print("Loading and cleaning data...")
        df = pd.read_csv(filepath, usecols=['Date', 'Service', 'UsageQuantity'])

        # Clean and transform data
        df = df.dropna(subset=['UsageQuantity'])
        df['Date'] = pd.to_datetime(df['Date'])
        df['UsageQuantity'] = pd.to_numeric(df['UsageQuantity'], errors='coerce')
        df = df.dropna(subset=['UsageQuantity'])

        # Add derived time features
        df['DayOfWeek'] = df['Date'].dt.day_name()
        df['Month'] = df['Date'].dt.month_name()
        df['Week'] = df['Date'].dt.isocalendar().week
        df['Day'] = df['Date'].dt.day

        print(f"Loaded {len(df)} records spanning {df['Date'].min().date()} to {df['Date'].max().date()}")
        return df

    except Exception as e:
        print(f"Error loading data: {e}")
        exit()


def plot_usage_distribution(df):
    """Plot enhanced distribution views of usage quantities"""
    print("Creating usage distribution plots...")

    # Create figure with subplots
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 16))

    # Filter out zeros for better visualization
    plot_data = df[df['UsageQuantity'] > 0]['UsageQuantity']

    # 1. Combined Histogram and KDE (Log Scale)
    sns.histplot(data=plot_data, bins=50, log_scale=(True, True), ax=ax1,
                 edgecolor='none', alpha=0.5, color='#1f77b4', stat='density')
    sns.kdeplot(data=plot_data, log_scale=(True, False),
                color='red', linewidth=2, ax=ax1)

    ax1.set_title("AWS Usage Distribution with Density Estimate", pad=15)
    ax1.set_xlabel("Usage Quantity (Log Scale)")
    ax1.set_ylabel("Density")

    # Add percentiles
    for p in [25, 50, 75, 95, 99]:
        percentile = np.percentile(plot_data, p)
        ax1.axvline(percentile, color='green', linestyle='--', alpha=0.7)
        ax1.text(percentile, ax1.get_ylim()[1] * 0.9,
                 f'{p}% ({human_format(percentile)})',
                 rotation=90, va='top', ha='right')

    # 2. Boxplot of log-transformed data
    log_data = np.log10(plot_data)
    sns.boxplot(x=log_data, ax=ax2, color='#ff7f0e', width=0.3)
    ax2.set_title("Log-Transformed Usage Distribution (Boxplot)", pad=15)
    ax2.set_xlabel("Log10(Usage Quantity)")
    ax2.set_ylabel("Distribution")

    plt.tight_layout()
    plt.savefig('usage_distribution.png', dpi=300, bbox_inches='tight')
    plt.close()


def plot_top_services(df, n=10):
    """Plot horizontal bar chart of top services"""
    print(f"Creating top {n} services visualization...")

    top_services = df.groupby('Service')['UsageQuantity'].sum().nlargest(n)
    plot_data = top_services.reset_index()
    plot_data.columns = ['Service', 'TotalUsage']

    plt.figure(figsize=(14, 8))

    # Create barplot with proper hue assignment
    ax = sns.barplot(
        x='TotalUsage',
        y='Service',
        data=plot_data,
        hue='Service',
        palette='viridis',
        edgecolor='.3',
        dodge=False,
        legend=False
    )

    plt.title(f"Top {n} Services by Total Usage", pad=20)
    plt.xlabel("Total Usage Quantity")
    plt.ylabel("")

    # Format x-axis with human-readable numbers
    ax.xaxis.set_major_formatter(FuncFormatter(lambda x, _: human_format(x)))

    # Add value annotations
    max_val = top_services.max()
    for i, (service, val) in enumerate(zip(top_services.index, top_services.values)):
        ax.text(
            val + max_val * 0.01,
            i,
            f"{human_format(val)}",
            va='center',
            fontsize=10
        )

    plt.subplots_adjust(left=0.3)
    plt.savefig('top_services.png', dpi=300, bbox_inches='tight')
    plt.close()


def plot_time_series(df, n=5):
    """Plot time series of top services"""
    print(f"Creating time series for top {n} services...")

    top_services = df.groupby('Service')['UsageQuantity'].sum().nlargest(n).index
    ts_data = df[df['Service'].isin(top_services)].pivot_table(
        index='Date', columns='Service', values='UsageQuantity', aggfunc='sum'
    ).fillna(0)

    plt.figure(figsize=(16, 9))
    palette = sns.color_palette("husl", n_colors=n)

    for i, service in enumerate(top_services):
        plt.plot(ts_data.index, ts_data[service],
                 marker='o', markersize=4, linewidth=2,
                 color=palette[i], label=service)

    plt.title(f"Daily Usage Trend for Top {n} Services", pad=20)
    plt.xlabel("Date")
    plt.ylabel("Usage Quantity")

    # Format x-axis
    ax = plt.gca()
    ax.xaxis.set_major_locator(mdates.WeekdayLocator(interval=1))
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%b %d'))
    plt.xticks(rotation=45)

    plt.legend(title='Service', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig('time_series.png', dpi=300, bbox_inches='tight')
    plt.close()


def plot_usage_heatmap(df):
    """Create a heatmap of weekly usage patterns"""
    print("Creating usage heatmap...")

    top_service = df.groupby('Service')['UsageQuantity'].sum().idxmax()
    heat_data = df[df['Service'] == top_service].copy()

    # Create pivot table of daily usage
    heat_data = heat_data.pivot_table(
        index='DayOfWeek',
        columns=pd.Grouper(key='Date', freq='W-MON'),
        values='UsageQuantity',
        aggfunc='sum'
    )

    # Order days properly
    days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
    heat_data = heat_data.reindex(days)

    plt.figure(figsize=(16, 8))
    sns.heatmap(
        heat_data,
        cmap='YlOrRd',
        linewidths=.5,
        annot=True,
        fmt='.1f',
        cbar_kws={'label': 'Usage Quantity'},
        annot_kws={'size': 8}
    )

    plt.title(f"Weekly Usage Heatmap for {top_service}", pad=20)
    plt.xlabel("Week Starting")
    plt.ylabel("Day of Week")
    plt.xticks(rotation=45, ha='right')
    plt.tight_layout()
    plt.savefig('usage_heatmap.png', dpi=300, bbox_inches='tight')
    plt.close()


def generate_summary_report(df):
    """Generate text summary report"""
    print("Generating summary report...")

    summary_stats = {
        'Report Generated': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'Time Period Covered': f"{df['Date'].min().strftime('%Y-%m-%d')} to {df['Date'].max().strftime('%Y-%m-%d')}",
        'Total Services Tracked': df['Service'].nunique(),
        'Total Records Processed': len(df),
        'Total Usage Quantity': f"{human_format(df['UsageQuantity'].sum())} ({df['UsageQuantity'].sum():,.2f})",
        'Average Daily Usage': human_format(df.groupby('Date')['UsageQuantity'].sum().mean()),
        'Peak Usage Day': f"{df.groupby('Date')['UsageQuantity'].sum().idxmax().strftime('%Y-%m-%d')} "
                          f"({human_format(df.groupby('Date')['UsageQuantity'].sum().max())})",
        'Most Used Service': f"{df.groupby('Service')['UsageQuantity'].sum().idxmax()} "
                             f"({human_format(df.groupby('Service')['UsageQuantity'].sum().max())})",
        'Zero Usage Records': f"{len(df[df['UsageQuantity'] == 0])} ({len(df[df['UsageQuantity'] == 0]) / len(df):.1%})"
    }

    with open('aws_usage_summary.txt', 'w') as f:
        f.write("AWS USAGE ANALYSIS REPORT\n")
        f.write("=" * 40 + "\n\n")
        for key, value in summary_stats.items():
            f.write(f"{key:<25}: {value}\n")

        f.write("\nTOP SERVICES SUMMARY:\n")
        f.write("-" * 40 + "\n")
        top_services = df.groupby('Service')['UsageQuantity'].sum().nlargest(10)
        for service, usage in top_services.items():
            f.write(f"{service:<30}: {human_format(usage):>10} ({usage:,.2f})\n")


def main():
    """Main execution function"""
    input_file = 'usage-report-2.csv'

    try:
        # Load and process data
        df = load_and_clean_data(input_file)

        # Generate visualizations
        plot_usage_distribution(df)
        plot_top_services(df)
        plot_time_series(df)
        plot_usage_heatmap(df)

        # Generate report
        generate_summary_report(df)

        print("\nAnalysis complete! Generated files:")
        print("- usage_distribution.png : Usage distribution charts")
        print("- top_services.png       : Top services by usage")
        print("- time_series.png        : Daily usage trends")
        print("- usage_heatmap.png      : Weekly usage patterns")
        print("- aws_usage_summary.txt  : Summary statistics")

    except Exception as e:
        print(f"\nError during analysis: {e}")
        exit(1)

if __name__ == "__main__":
    main()
