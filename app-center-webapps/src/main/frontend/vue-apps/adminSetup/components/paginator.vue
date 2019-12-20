<template>
    <ul class="paginator">
        <li class="paginator-item prev-page">
            <button
                    :disabled="isInFirstPage"
                    @click="onClickPreviousPage"
                    type="button">
                <img src="/app-center/skin/images/paginator/previous-page.svg">
            </button>
        </li>

        <li class="paginator-item" v-for="page in pages">
            <button
                    :class="{ isActive: isPageActive(page.name) }"
                    :disabled="page.isDisabled"
                    @click="onClickPage(page.name)"
                    type="button">
                {{ page.name }}
            </button>
        </li>

        <li class="paginator-item next-page">
            <button
                    :disabled="isInLastPage"
                    @click="onClickNextPage"
                    type="button">
                <img src="/app-center/skin/images/paginator/next-page.svg">
            </button>
        </li>
    </ul>

</template>
<script>
    export default {
        name: "paginator",
        props: {
            maxVisibleButtons: {
                type: Number,
                required: false,
                default: 10
            },
            totalPages: {
                type: Number,
                required: true
            },
            total: {
                type: Number,
                required: true
            },
            perPage: {
                type: Number,
                required: true
            },
            currentPage: {
                type: Number,
                required: true
            },
        },
        computed: {
            startPage() {
                if (this.currentPage == 1) {
                    return 1;
                }

                if (this.currentPage == this.totalPages) {
                    if(this.totalPages - this.maxVisibleButtons +1 < 0)
                    return 1;
                    else
                        return this.totalPages - this.maxVisibleButtons +1;
                }

                if (this.currentPage < this.maxVisibleButtons)
                {
                    return 1;
                }
                else if(this.totalPages - this.currentPage < this.maxVisibleButtons)
                {
                    return this.totalPages - this.maxVisibleButtons +1  ;
                }


                return this.currentPage ;

            },
            endPage() {

                return Math.min(this.startPage + this.maxVisibleButtons - 1, this.totalPages);

            },
            pages() {
                const range = [];

                for (let i = this.startPage; i <= this.endPage; i += 1) {
                    range.push({
                        name: i,
                        isDisabled: i == this.currentPage
                    });
                }

                return range;
            },
            isInFirstPage() {
                return this.currentPage == 1;
            },
            isInLastPage() {
                return this.currentPage == this.totalPages;
            },
        },
        methods: {
            onClickFirstPage() {
                this.$emit('pagechanged', 1);
            },
            onClickPreviousPage() {
                this.$emit('pagechanged', this.currentPage - 1);
            },
            onClickPage(page) {
                this.$emit('pagechanged', page);
            },
            onClickNextPage() {
                this.$emit('pagechanged', this.currentPage + 1);
            },
            onClickLastPage() {
                this.$emit('pagechanged', this.totalPages);
            },
            isPageActive(page) {
                return this.currentPage == page;
            }
        }
    }

</script>
<style scoped>
	.paginator {
		text-align: center;
    }
    
    .paginator-item {
		display: inline-block;
		font-size: 17px;
  		font-weight: 700;
    }
    
    .paginator-item button {
		border: none;
		color: #989898;
		background: transparent;
    }
    
    .paginator-item button:hover {
		color: #EE7203;
    }

    .paginator-item button:focus {
        outline: transparent auto 5px;
    }
    .isActive {
        color: #7C4091 !important;
        border-radius: 4px;
    }
    
    .next-page button {
        padding-left: 20px;
    }
    
    .prev-page button {
        padding-right: 20px;
    }
</style>
